#include <stdio.h>
#include <stdlib.h>

#include <sys/types.h>
// #include<sys/socket.h>  for unix

// for windows
#include <winsock2.h>
#include <ws2tcpip.h>
// Link with the Ws2_32.lib library
#pragma comment(lib, "ws2_32.lib")

int main()
{
    // for windows ---------------------------
    // Initialize Winsock
    WSADATA wsaData;
    int result = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (result != 0)
    {
        fprintf(stderr, "WSAStartup failed with error: %d\n", result);
        return 1;
    }

    printf("Winsock initialized successfully!\n");
    // ---------------------------------------
    int port = 9090;
    char *hostname;
    connect(hostname,port);
    // for windows ---------------------------
    // Clean up Winsock
    WSACleanup();

    printf("Winsock cleaned up.\n");
    // ---------------------------------------
    return 0;
}
int connect(char *hostname, int port)
{
    // 1.1 create a socket
    int sock;
    // socket(int family , int type, int protocol);
    sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock == -1)
        return sock; // -1 = error

    // 1.2. Resolve the host
    /*Return nonnull pointer if OK, NULL on error */
    struct hostent *host;
    host = gethostbyname(hostname);
    if (host == NULL)
    {
        close(sock);
        return -1;
    }

    // 2. Setup the server address
    struct sockaddr_in server_address;
    memset(&server_address, 0, size(server_address));
    server_address.sin_family = AF_INET;
    /* unit16_t htons(unit16_t host16bitvaule)
    Change the port number from host byte order to network byte order */
    server_address.sin_port = htons(port);
    // server_address.sin_addr.s_addr = INADDR_ANY; // 0.0.0.0
    server_address.sin_addr.s_addr = *(unsigned long *)host->h_addr_list[0]; // host binding

    // 3. Connect to server
    /*Perform the TCP three way handshaking*/
    if (connect(sock, (struct sockaddr *)&server_address, sizeof(server_address)) != 0)
    {
        close(sock);
        return -1;
    }

    return sock;
}