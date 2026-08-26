# PBLOCK - Family Safety (Windows GUI)
# Build with MinGW-w64 cross-compiler on Linux:
#   sudo apt install g++-mingw-w64-x86-64
#   make
# Or build natively on Windows with MSVC:
#   cl /EHsc main.cpp /link user32.lib gdi32.lib comctl32.lib crypt32.lib

CXX = x86_64-w64-mingw32-g++
CXXFLAGS = -std=c++17 -O2 -Wall -mwindows -municode
LIBS = -lcomctl32 -lgdi32 -lcrypt32
TARGET = pblock-gui.exe

all: $(TARGET)

$(TARGET): main.cpp
	$(CXX) $(CXXFLAGS) -o $(TARGET) main.cpp $(LIBS)

clean:
	rm -f $(TARGET) *.o
