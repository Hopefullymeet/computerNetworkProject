@echo off
echo Testing DNS Relay Implementation
echo ================================

echo.
echo Compiling DNS Relay...
javac src/main/java/DnsRelayBasic.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b 1
)

echo.
echo Compiling test client...
javac test_dns_relay.java
if %errorlevel% neq 0 (
    echo Test client compilation failed!
    exit /b 1
)

echo.
echo Available command-line options:
echo   java DnsRelayBasic [-d] [-dd] [-4] [-6] [dns-server-ip] [db-file]
echo.
echo   -d    : Enable debug output
echo   -dd   : Enable debug output and packet dump
echo   -4    : Return only IPv4 (A) records
echo   -6    : Return only IPv6 (AAAA) records
echo   (default: return both IPv4 and IPv6 records)
echo.
echo Examples:
echo   java -cp src/main/java DnsRelayBasic -d 8.8.8.8 src/main/java/dnsrelay.txt
echo   java -cp src/main/java DnsRelayBasic -d -4 8.8.8.8 src/main/java/dnsrelay.txt
echo   java -cp src/main/java DnsRelayBasic -d -6 8.8.8.8 src/main/java/dnsrelay.txt
echo.
echo To test the implementation:
echo 1. Run the DNS relay in one terminal
echo 2. Run the test client in another terminal: java test_dns_relay
echo.
echo Key improvements implemented:
echo - Concurrent request processing using multiple threads
echo - Enhanced ID management for upstream forwarding with thread safety
echo - Command-line address type filtering (-4 for IPv4 only, -6 for IPv6 only)
echo - Proper timeout handling and cleanup of expired requests
echo - Graceful shutdown handling
echo.
echo The implementation maintains low-level socket operations as required.
