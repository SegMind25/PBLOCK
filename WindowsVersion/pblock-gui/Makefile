# PBLOCK - Family Safety (Windows GUI)
# Build with MinGW-w64 cross-compiler on Linux:
#   sudo apt install g++-mingw-w64-x86-64
#   make
# Or build natively on Windows with MSVC:
#   rc pblock.rc && cl /EHsc main.cpp pblock.res /link user32.lib gdi32.lib comctl32.lib crypt32.lib

CXX = x86_64-w64-mingw32-g++
WINDRES = x86_64-w64-mingw32-windres
CXXFLAGS = -std=c++17 -O2 -Wall -mwindows
LIBS = -lcomctl32 -lgdi32 -lcrypt32
TARGET = pblock-gui.exe

all: $(TARGET)

$(TARGET): main.cpp pblock_res.o
	$(CXX) $(CXXFLAGS) -o $(TARGET) main.cpp pblock_res.o $(LIBS)

pblock_res.o: pblock.rc pblock.manifest
	$(WINDRES) pblock.rc -O coff -o pblock_res.o

clean:
	rm -f $(TARGET) *.o
