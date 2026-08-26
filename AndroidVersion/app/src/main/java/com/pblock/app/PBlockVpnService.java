package com.pblock.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class PBlockVpnService extends VpnService {

    private static final String TAG = "PBLOCK";
    public static final String ACTION_STOP = "com.pblock.app.VPN_STOP";

    private static final String VPN_CHANNEL_ID = "pblock_vpn";
    private static final int VPN_NOTIFICATION_ID = 2;

    private static final String FAKE_DNS_V4 = "10.111.222.3";
    private static final String UPSTREAM_DNS_V4 = "8.8.8.8";
    private static final String GOOGLE_SAFESEARCH_IP = "216.239.38.120";
    private static final String BING_STRICT_IP = "204.79.197.220";
    private static final int MTU = 1500;
    private static final int QTYPE_A = 1;

    private static final AtomicBoolean running = new AtomicBoolean(false);

    private Thread workerThread;
    private ParcelFileDescriptor tunInterface;

    private static HashSet<String> blockedSet;
    private static HashSet<String> googleSet;
    private static HashSet<String> bingSet;

    public static boolean isRunning() {
        return running.get();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildDomainSets();
        createVpnNotificationChannel();
    }

    private void createVpnNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                VPN_CHANNEL_ID,
                "Family Safety",
                NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows while content filtering is active");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, VPN_CHANNEL_ID)
                .setContentTitle("Family Safety Active")
                .setContentText("Content filtering is protecting this device")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();
        } else {
            notification = new Notification.Builder(this)
                .setContentTitle("Family Safety Active")
                .setContentText("Content filtering is protecting this device")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();
        }
        try {
            startForeground(VPN_NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "startForeground failed: " + e.getMessage());
        }

        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopVpnInternal();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!running.get()) {
            startVpnInternal();
        }
        return START_STICKY;
    }

    private synchronized void startVpnInternal() {
        try {
            VpnService.Builder builder = new VpnService.Builder()
                .setSession("PBLOCK DNS Blocker")
                .setMtu(MTU)
                .addAddress("10.111.222.1", 32)
                .addDnsServer(FAKE_DNS_V4)
                .addRoute(FAKE_DNS_V4, 32)
                .addAddress("fdfe:dcba:9876::1", 126)
                .addDnsServer("fdfe:dcba:9876::3")
                .addRoute("fdfe:dcba:9876::3", 128);

            tunInterface = builder.establish();
            if (tunInterface == null) {
                Log.e(TAG, "VPN establish failed");
                stopSelf();
                return;
            }

            running.set(true);

            workerThread = new Thread(this::runLoop, "PBlockVpnWorker");
            workerThread.start();
            Log.i(TAG, "VPN started");
        } catch (Exception e) {
            Log.e(TAG, "VPN start error: " + e.getMessage());
            running.set(false);
            stopSelf();
        }
    }

    private void runLoop() {
        byte[] packet = new byte[MTU];
        byte[] fakeDnsV4 = parseIp(FAKE_DNS_V4);

        try (FileInputStream in = new FileInputStream(tunInterface.getFileDescriptor());
             FileOutputStream out = new FileOutputStream(tunInterface.getFileDescriptor())) {

            while (running.get() && !Thread.currentThread().isInterrupted()) {
                int length = in.read(packet);
                if (length <= 20) continue;

                int version = (packet[0] >> 4) & 0xF;
                if (version == 4) {
                    handleIpv4(packet, length, fakeDnsV4, out);
                } else if (version == 6) {
                    handleIpv6(packet, length, out);
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                Log.e(TAG, "VPN loop error: " + e.getMessage());
            }
        }
    }

    private void handleIpv4(byte[] packet, int length, byte[] fakeDns, FileOutputStream out) {
        if (length < 28) return;
        int ihl = (packet[0] & 0x0F) * 4;
        int protocol = packet[9] & 0xFF;
        if (protocol != 17 || ihl < 20 || length < ihl + 8) return;

        byte[] srcAddr = Arrays.copyOfRange(packet, 12, 16);
        byte[] dstAddr = Arrays.copyOfRange(packet, 16, 20);
        if (!Arrays.equals(dstAddr, fakeDns)) return;

        int udpLen = ((packet[ihl + 4] & 0xFF) << 8) | (packet[ihl + 5] & 0xFF);
        int srcPort = ((packet[ihl] & 0xFF) << 8) | (packet[ihl + 1] & 0xFF);
        int destPort = ((packet[ihl + 2] & 0xFF) << 8) | (packet[ihl + 3] & 0xFF);
        if (destPort != 53 || udpLen < 12) return;

        byte[] dnsQuery = Arrays.copyOfRange(packet, ihl + 8,
            Math.min(length, ihl + udpLen));

        DnsResult result = processDns(dnsQuery);
        byte[] payload;

        if (result.blocked) {
            payload = result.qtype == QTYPE_A
                ? buildResponseWithARecord(dnsQuery, "0.0.0.0")
                : buildEmptyNoErrorResponse(dnsQuery);
        } else if (result.answerIp != null && result.qtype == QTYPE_A) {
            payload = buildResponseWithARecord(dnsQuery, result.answerIp);
        } else if (result.answerIp != null) {
            payload = buildEmptyNoErrorResponse(dnsQuery);
        } else {
            payload = forwardUpstream(dnsQuery);
            if (payload == null) return;
        }

        writeIpv4Udp(payload, fakeDns, srcAddr, (short) 53, (short) srcPort, out);
    }

    private void handleIpv6(byte[] packet, int length, FileOutputStream out) {
        if (length < 48) return;
        int nextHeader = packet[6] & 0xFF;
        if (nextHeader != 17) return;

        byte[] srcAddr = Arrays.copyOfRange(packet, 8, 24);
        int udpStart = 40;
        int udpLen = ((packet[udpStart + 4] & 0xFF) << 8) | (packet[udpStart + 5] & 0xFF);
        int srcPort = ((packet[udpStart] & 0xFF) << 8) | (packet[udpStart + 1] & 0xFF);
        int destPort = ((packet[udpStart + 2] & 0xFF) << 8) | (packet[udpStart + 3] & 0xFF);
        if (destPort != 53 || udpLen < 12) return;

        byte[] dnsQuery = Arrays.copyOfRange(packet, udpStart + 8,
            Math.min(length, udpStart + udpLen));

        DnsResult result = processDns(dnsQuery);
        byte[] payload;

        if (result.blocked || result.answerIp != null) {
            payload = buildEmptyNoErrorResponse(dnsQuery);
        } else {
            payload = forwardUpstream(dnsQuery);
            if (payload == null) return;
        }

        writeIpv6Udp(payload, srcAddr, (short) 53, (short) srcPort, out);
    }

    private DnsResult processDns(byte[] dns) {
        DnsResult result = new DnsResult();
        try {
            int qdcount = ((dns[4] & 0xFF) << 8) | (dns[5] & 0xFF);
            if (qdcount < 1 || dns.length < 17) return result;
            int[] offset = {12};
            String qname = readQName(dns, offset);
            if (qname == null || qname.isEmpty() || offset[0] + 4 > dns.length) return result;
            int qtype = ((dns[offset[0]] & 0xFF) << 8) | (dns[offset[0] + 1] & 0xFF);
            result.qname = qname;
            result.qtype = qtype;

            String lower = qname.toLowerCase(Locale.US);
            if (matchesSet(lower, googleSet)) {
                result.answerIp = GOOGLE_SAFESEARCH_IP;
            } else if (matchesSet(lower, bingSet)) {
                result.answerIp = BING_STRICT_IP;
            } else if (matchesSet(lower, blockedSet)) {
                result.blocked = true;
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private boolean matchesSet(String qname, HashSet<String> set) {
        for (String domain : set) {
            if (qname.equals(domain) || qname.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }

    private String readQName(byte[] dns, int[] offset) {
        StringBuilder sb = new StringBuilder();
        int len;
        while ((len = dns[offset[0]] & 0xFF) != 0) {
            if ((len & 0xC0) != 0 || offset[0] + len + 1 >= dns.length) return null;
            for (int i = 1; i <= len; i++) {
                sb.append((char) (dns[offset[0] + i] & 0xFF));
            }
            sb.append('.');
            offset[0] += len + 1;
        }
        offset[0] += 1;
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private byte[] forwardUpstream(byte[] dnsQuery) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(3000);
            protect(socket);

            InetAddress addr = InetAddress.getByName(UPSTREAM_DNS_V4);
            socket.send(new DatagramPacket(dnsQuery, dnsQuery.length, addr, 53));

            byte[] buf = new byte[2048];
            DatagramPacket response = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(response);
            } catch (SocketTimeoutException e) {
                return null;
            }
            return Arrays.copyOf(response.getData(), response.getLength());
        } catch (Exception e) {
            return null;
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private byte[] buildResponseWithARecord(byte[] query, String ipStr) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int qEnd = findQuestionEnd(query);
            if (qEnd < 0) return buildEmptyNoErrorResponse(query);

            out.write(Arrays.copyOfRange(query, 0, 12));
            out.write((byte) 0x81);
            out.write((byte) 0x80);
            out.write(query[4]);
            out.write(query[5]);
            out.write((byte) 0x00);
            out.write((byte) 0x01);
            out.write((byte) 0x00);
            out.write((byte) 0x00);
            out.write((byte) 0x00);
            out.write((byte) 0x00);
            out.write(Arrays.copyOfRange(query, 12, qEnd));
            out.write((byte) 0xC0);
            out.write((byte) 0x0C);
            out.write((byte) 0x00);
            out.write((byte) 0x01);
            out.write((byte) 0x00);
            out.write((byte) 0x01);
            out.write((byte) 0x00);
            out.write((byte) 0x00);
            out.write((byte) 0x00);
            out.write((byte) 0x3C);
            out.write((byte) 0x00);
            out.write((byte) 0x04);
            out.write(parseIp(ipStr));
            return out.toByteArray();
        } catch (Exception e) {
            return buildEmptyNoErrorResponse(query);
        }
    }

    private byte[] buildEmptyNoErrorResponse(byte[] query) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int qEnd = findQuestionEnd(query);
            out.write(Arrays.copyOfRange(query, 0, 12));
            out.write((byte) 0x81);
            out.write((byte) 0x80);
            out.write(query[4]);
            out.write(query[5]);
            out.write((byte) 0x00);
            out.write((byte) 0x00);
            out.write((byte) 0x00);
            out.write((byte) 0x00);
            out.write((byte) 0x00);
            out.write((byte) 0x00);
            if (qEnd > 0) {
                out.write(Arrays.copyOfRange(query, 12, qEnd));
            }
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[]{0, 0, (byte) 0x81, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0};
        }
    }

    private int findQuestionEnd(byte[] dns) {
        int pos = 12;
        try {
            while ((dns[pos] & 0xFF) != 0) {
                pos += (dns[pos] & 0xFF) + 1;
            }
            return pos + 5;
        } catch (Exception e) {
            return -1;
        }
    }

    private void writeIpv4Udp(byte[] payload, byte[] srcAddr, byte[] dstAddr, short srcPort,
                              short dstPort, FileOutputStream out) {
        try {
            int totalLen = 20 + 8 + payload.length;
            ByteBuffer bb = ByteBuffer.allocate(totalLen);
            bb.put((byte) 0x45);
            bb.put((byte) 0x00);
            bb.putShort((short) totalLen);
            bb.putShort((short) 0);
            bb.putShort((short) 0x4000);
            bb.put((byte) 64);
            bb.put((byte) 17);
            bb.putShort((short) 0);
            bb.put(srcAddr);
            bb.put(dstAddr);
            int ipChecksum = checksum(bb.array(), 0, 20);
            bb.putShort(10, (short) ipChecksum);
            bb.putShort(srcPort);
            bb.putShort(dstPort);
            bb.putShort((short) (8 + payload.length));
            bb.putShort((short) 0);
            bb.put(payload);
            out.write(bb.array());
        } catch (Exception e) {
            Log.w(TAG, "write v4 error: " + e.getMessage());
        }
    }

    private void writeIpv6Udp(byte[] payload, byte[] dstAddr, short srcPort, short dstPort,
                              FileOutputStream out) {
        try {
            int totalLen = 40 + 8 + payload.length;
            ByteBuffer bb = ByteBuffer.allocate(totalLen);
            bb.put((byte) 0x60);
            bb.put((byte) 0x00);
            bb.putShort((short) payload.length);
            bb.put((byte) 17);
            bb.put((byte) 64);
            bb.put(parseIp("fdfe:dcba:9876::3"));
            bb.put(dstAddr);
            bb.putShort(srcPort);
            bb.putShort(dstPort);
            bb.putShort((short) (8 + payload.length));
            bb.putShort((short) 0);
            bb.put(payload);
            out.write(bb.array());
        } catch (Exception e) {
            Log.w(TAG, "write v6 error: " + e.getMessage());
        }
    }

    private int checksum(byte[] data, int offset, int length) {
        int sum = 0;
        for (int i = 0; i < length - 1; i += 2) {
            sum += ((data[offset + i] & 0xFF) << 8) | (data[offset + i + 1] & 0xFF);
        }
        if (length % 2 == 1) {
            sum += (data[offset + length - 1] & 0xFF) << 8;
        }
        while ((sum >> 16) != 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }
        return ~sum & 0xFFFF;
    }

    private byte[] parseIp(String ip) {
        try {
            return InetAddress.getByName(ip).getAddress();
        } catch (Exception e) {
            return new byte[]{0, 0, 0, 0};
        }
    }

    private static synchronized void buildDomainSets() {
        if (blockedSet != null) return;
        blockedSet = sanitize(PBlockHelper.getBlockedDomainsForVpn());
        googleSet = sanitize(PBlockHelper.getSafesearchDomainsForVpn(true));
        bingSet = sanitize(PBlockHelper.getSafesearchDomainsForVpn(false));
    }

    private static HashSet<String> sanitize(java.util.List<String> list) {
        HashSet<String> out = new HashSet<>();
        for (String d : list) {
            String clean = d.trim().toLowerCase(Locale.US);
            if (clean.contains("/") || clean.isEmpty()) continue;
            out.add(clean);
        }
        return out;
    }

    @Override
    public void onRevoke() {
        stopVpnInternal();
        stopSelf();
    }

    private synchronized void stopVpnInternal() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
        try {
            if (tunInterface != null) {
                tunInterface.close();
            }
        } catch (Exception ignored) {
        }
        tunInterface = null;
        Log.i(TAG, "VPN stopped");
    }

    @Override
    public void onDestroy() {
        stopVpnInternal();
        super.onDestroy();
    }

    private static class DnsResult {
        String qname;
        int qtype;
        boolean blocked;
        String answerIp;
    }
}
