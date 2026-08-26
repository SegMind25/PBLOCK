CXX = g++
CXXFLAGS = -std=c++17 -O2 -Wall $(shell pkg-config --cflags gtk+-3.0)
LDFLAGS = $(shell pkg-config --libs gtk+-3.0) -lpthread -lcrypt
PREFIX = /usr
DESTDIR =
APP_NAME = pblock
VERSION = 1.0

SRC = src/main.cpp
OBJ = $(SRC:.cpp=.o)
BIN = pblock

all: $(BIN)

$(BIN): $(SRC)
	$(CXX) $(CXXFLAGS) -o $@ $< $(LDFLAGS)

clean:
	rm -f $(BIN) $(OBJ)

install: $(BIN)
	install -Dm755 $(BIN) $(DESTDIR)$(PREFIX)/bin/$(APP_NAME)
	install -Dm644 desktop/pblock.desktop $(DESTDIR)$(PREFIX)/share/applications/pblock.desktop
	install -Dm644 icons/pblock.png $(DESTDIR)$(PREFIX)/share/icons/hicolor/256x256/apps/pblock.png
	install -Dm644 LICENSE $(DESTDIR)$(PREFIX)/share/doc/pblock/copyright

uninstall:
	rm -f $(DESTDIR)$(PREFIX)/bin/$(APP_NAME)
	rm -f $(DESTDIR)$(PREFIX)/share/applications/pblock.desktop
	rm -f $(DESTDIR)$(PREFIX)/share/icons/hicolor/256x256/apps/pblock.png
	rm -rf $(DESTDIR)$(PREFIX)/share/doc/pblock

deb: $(BIN)
	@mkdir -p deb-pkg/DEBIAN
	@mkdir -p deb-pkg/usr/bin
	@mkdir -p deb-pkg/usr/share/applications
	@mkdir -p deb-pkg/usr/share/icons/hicolor/256x256/apps
	@mkdir -p deb-pkg/usr/share/doc/pblock
	@cp $(BIN) deb-pkg/usr/bin/pblock
	@chmod 755 deb-pkg/usr/bin/pblock
	@cp desktop/pblock.desktop deb-pkg/usr/share/applications/
	@cp icons/pblock.png deb-pkg/usr/share/icons/hicolor/256x256/apps/
	@cp LICENSE deb-pkg/usr/share/doc/pblock/copyright
	@cat debian/control | sed 's/VERSION/$(VERSION)/g' > deb-pkg/DEBIAN/control
	@cp debian/postinst deb-pkg/DEBIAN/postinst
	@cp debian/prerm deb-pkg/DEBIAN/prerm
	@chmod 755 deb-pkg/DEBIAN/postinst deb-pkg/DEBIAN/prerm
	@fakeroot dpkg-deb --build deb-pkg pblock_$(VERSION)_amd64.deb
	@rm -rf deb-pkg
	@echo "Built: pblock_$(VERSION)_amd64.deb"

.PHONY: all clean install uninstall deb
