@echo off
echo Modular DNS Relay Build and Run Script
echo ======================================

echo.
echo Compiling all modules...
javac src/main/java/*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b 1
)
echo ✅ All modules compiled successfully

echo.
echo Compiling test programs...
javac -cp src/main/java test_modular_dns_relay.java
if %errorlevel% neq 0 (
    echo Test compilation failed!
    exit /b 1
)
echo ✅ Test programs compiled successfully

echo.
echo Module Structure:
echo ================
echo 📁 src/main/java/
echo   ├── DnsRelayBasic.java      (Main controller)
echo   ├── DnsMessage.java         (RFC1034 DNS message structure)
echo   ├── DnsMessageUtils.java    (DNS message utilities)
echo   ├── AddressFilter.java      (IPv4/IPv6 address filtering)
echo   ├── UdpSocketWrapper.java   (UDP communication wrapper)
echo   ├── IdMappingManager.java   (ID mapping management)
echo   ├── DnsLogger.java          (Unified logging)
echo   └── LocalDatabase.java      (Local DNS database)

echo.
echo Usage Examples:
echo ==============
echo.
echo 1. Basic run:
echo    java -cp src/main/java DnsRelayBasic 8.8.8.8 src/main/java/dnsrelay.txt
echo.
echo 2. Debug mode:
echo    java -cp src/main/java DnsRelayBasic -d 8.8.8.8 src/main/java/dnsrelay.txt
echo.
echo 3. Packet dump mode:
echo    java -cp src/main/java DnsRelayBasic -dd 8.8.8.8 src/main/java/dnsrelay.txt
echo.
echo 4. IPv4 only mode:
echo    java -cp src/main/java DnsRelayBasic -d -4 8.8.8.8 src/main/java/dnsrelay.txt
echo.
echo 5. IPv6 only mode:
echo    java -cp src/main/java DnsRelayBasic -d -6 8.8.8.8 src/main/java/dnsrelay.txt
echo.
echo 6. Test modular implementation:
echo    java -cp src/main/java test_modular_dns_relay

echo.
echo Key Features:
echo ============
echo ✅ Modular design with clear separation of concerns
echo ✅ RFC1034 compliant DNS message parsing
echo ✅ Thread pool based concurrent processing
echo ✅ Basic UDP system calls (sendto/recvfrom)
echo ✅ Reliable ID mapping for upstream forwarding
echo ✅ Address type filtering (IPv4/IPv6)
echo ✅ Clear communication logging (Resolver/DNS Relay/DNS Server)
echo ✅ Distinction between intercepted and not found domains
echo ✅ Low-level implementation without high-level functions

echo.
echo Communication Flow:
echo ==================
echo Resolver to DNS Relay: Client queries
echo DNS Relay to Resolver: Local responses or interceptions
echo DNS Relay to DNS Server: Upstream forwarding
echo DNS Server to DNS Relay: Upstream responses
echo DNS Relay to Resolver: Final responses with restored IDs

echo.
echo Ready to run! Choose one of the usage examples above.
echo For testing, run: java -cp src/main/java test_modular_dns_relay
