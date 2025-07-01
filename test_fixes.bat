@echo off
echo DNS Relay Fixes Verification
echo ============================

echo.
echo Compiling updated DNS Relay...
javac src/main/java/DnsRelayBasic.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b 1
)

echo.
echo Compiling test client...
javac test_address_filtering.java
if %errorlevel% neq 0 (
    echo Test client compilation failed!
    exit /b 1
)

echo.
echo FIXES IMPLEMENTED:
echo ==================
echo.
echo 1. ADDRESS FILTERING FIX:
echo    - Problem: -4 and -6 flags only filtered local database responses
echo    - Solution: Added address filtering to upstream DNS responses
echo    - Implementation: Enhanced Pending class to store query type
echo                     Added applyAddressFilter() method for upstream responses
echo.
echo 2. PACKET DUMP FORMATTING FIX:
echo    - Problem: Missing newline in dump output causing log messages to merge
echo    - Solution: Added proper newline handling in dump() method
echo    - Implementation: Ensure last line of hex dump always ends with newline
echo.
echo TESTING INSTRUCTIONS:
echo ====================
echo.
echo To test the address filtering fix:
echo.
echo 1. Start DNS relay with IPv4 only filtering:
echo    java -cp src/main/java DnsRelayBasic -d -4 8.8.8.8 src/main/java/dnsrelay.txt
echo.
echo 2. In another terminal, run the test:
echo    java test_address_filtering
echo.
echo 3. Expected behavior:
echo    - A record queries should succeed
echo    - AAAA record queries should return NXDOMAIN
echo.
echo 4. Test with IPv6 only filtering:
echo    java -cp src/main/java DnsRelayBasic -d -6 8.8.8.8 src/main/java/dnsrelay.txt
echo.
echo 5. Expected behavior:
echo    - AAAA record queries should succeed  
echo    - A record queries should return NXDOMAIN
echo.
echo To test the packet dump fix:
echo.
echo 1. Start DNS relay with packet dump:
echo    java -cp src/main/java DnsRelayBasic -dd 8.8.8.8 src/main/java/dnsrelay.txt
echo.
echo 2. Make some DNS queries and verify:
echo    - Hex dump lines are properly formatted
echo    - Log messages appear on separate lines
echo    - No text merging issues
echo.
echo KEY CHANGES MADE:
echo =================
echo.
echo 1. Enhanced Pending class:
echo    - Added qtype field to store original query type
echo    - Updated constructor to accept query type parameter
echo.
echo 2. Updated handleQuery method:
echo    - Pass query type when creating Pending objects
echo.
echo 3. Enhanced handleUpstreamResponse method:
echo    - Apply address filtering before relaying responses
echo    - Use applyAddressFilter() to check if response should be filtered
echo.
echo 4. Added applyAddressFilter method:
echo    - Check ADDRESS_FILTER setting against query type
echo    - Return NXDOMAIN for filtered query types
echo    - Preserve original responses for allowed types
echo.
echo 5. Fixed dump method:
echo    - Added conditional newline for incomplete hex lines
echo    - Ensures proper formatting of packet dumps
echo.
echo The fixes maintain low-level implementation requirements while
echo properly filtering both local and upstream DNS responses.
