#include <jni.h>
#include <iostream>
#include <arpa/inet.h>
#include <cstring>
#include "udt.h"

#ifndef _Included_HelloJNI
#define _Included_HelloJNI
#ifdef __cplusplus
extern "C" {
#endif

using namespace std;

JNIEXPORT void JNICALL Java_HelloJNI_sayHello(JNIEnv *env, jobject thisobj, jstring jhost, jint jport)
{
    const char *host = env->GetStringUTFChars(jhost, NULL);
    cout << "Start connect " << host << ":" << jport << endl;
UDTSOCKET client = UDT::socket(AF_INET, SOCK_STREAM, 0);

sockaddr_in serv_addr;
serv_addr.sin_family = AF_INET;
serv_addr.sin_port = htons(9000);
inet_pton(AF_INET, host, &serv_addr.sin_addr);

memset(&(serv_addr.sin_zero), '\0', 8);

// connect to the server, implict bind
if (UDT::ERROR == UDT::connect(client, (sockaddr*)&serv_addr, sizeof(serv_addr)))
{
  cout << "connect: " << UDT::getlasterror().getErrorMessage();
  return ;
}

const char* hello = "hello world!\n";
if (UDT::ERROR == UDT::send(client, hello, strlen(hello) + 1, 0))
{
  cout << "send: " << UDT::getlasterror().getErrorMessage();
  return ;
}

UDT::close(client);

    cout << "Connect success!" << endl;
    UDT::close(client);
    cout << "Hello World from C++ ,yup!" << endl;
    return;
}
#ifdef __cplusplus
}
#endif
#endif