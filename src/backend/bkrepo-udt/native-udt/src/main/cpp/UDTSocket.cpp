#include <jni.h>
#include <iostream>
#include <arpa/inet.h>
#include <cstring>
#include "udt.h"
#include "JNIHelpers.h"

#ifndef _Included_HelloJNI
#define _Included_HelloJNI
#ifdef __cplusplus
extern "C"
{
#endif
  using namespace std;

  static jclass udt_ExceptionUDT; // com.barchart.udt.ExceptionUDT
  static jmethodID udt_ExceptionUDT_init0;

  void UDT_InitClassRefAll(JNIEnv *const env)
  {
    X_InitClassReference(env, &udt_ExceptionUDT, //
                         "com/tencent/bkrepo/net/udt/UDTSocketException");
    X_InitClassReference(env, &jdk_InetAddress, "java/net/InetAddress");
  }
  void UDT_InitMethodRefAll( //
      JNIEnv *const env      //
  )
  {
    // InetAddress
    jdk_InetAddress_getAddress = env->GetMethodID(jdk_InetAddress, "getAddress",
                                                  "()[B");
    CHK_NUL_RET_RET(jdk_InetAddress_getAddress,
                    "jdk_clsInetAddress_getAddressID");

    udt_ExceptionUDT_init0 = env->GetMethodID(udt_ExceptionUDT, //
                                              "<init>", "(IILjava/lang/String;)V");
    CHK_NUL_RET_RET(udt_ExceptionUDT_init0, "udt_clsExceptionUDT_initID0");
  }

#define UDT_WRAPPER_MESSAGE -3 // WRAPPER_MESSAGE(-3, "wrapper generated error"), //

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
    if (UDT::ERROR == UDT::connect(client, (sockaddr *)&serv_addr, sizeof(serv_addr)))
    {
      cout << "connect: " << UDT::getlasterror().getErrorMessage();
      return;
    }

    const char *hello = "hello world!\n";
    if (UDT::ERROR == UDT::send(client, hello, strlen(hello) + 1, 0))
    {
      cout << "send: " << UDT::getlasterror().getErrorMessage();
      return;
    }

    UDT::close(client);

    cout << "Connect success!" << endl;
    UDT::close(client);
    cout << "Hello World from C++ ,yup!" << endl;
    return;
  }

  /*
   * Class:     com_tencent_bkrepo_net_udt_UDTSocket
   * Method:    socketCreate
   * Signature: (Z)I
   */
  JNIEXPORT jint JNICALL Java_com_tencent_bkrepo_net_udt_UDTSocket_socketCreate(JNIEnv *env, jobject thisobj, jboolean stream)
  {
    cout << "create" << endl;
    bool isStream = BOOL(stream);
    int socktype = isStream ? SOCK_STREAM : SOCK_DGRAM;
    UDTSOCKET socketfd = UDT::socket(AF_INET, socktype, 0);
    cout << socktype << ":" << socketfd << ":" << (jint)socketfd << endl;
    return (jint)socketfd;
  }

  /*
   * Class:     com_tencent_bkrepo_net_udt_UDTSocket
   * Method:    connect0
   * Signature: (ILjava/net/InetAddress;I)V
   */
  JNIEXPORT void JNICALL Java_com_tencent_bkrepo_net_udt_UDTSocket_connect0(JNIEnv *env, jobject thisobj, jint fd, jobject objRemoteAddress, jint port)
  {

    sockaddr remoteSockAddr;
    int rv;
    rv = X_InitSockAddr(&remoteSockAddr);
    if (rv == JNI_ERR)
    {
      UDT_ThrowExceptionUDT_Message(env, fd, "can not X_InitSockAddr");
      return;
    }

    rv = X_ConvertInetAddressAndPortToSockaddr(env, objRemoteAddress, port, &remoteSockAddr);

    if (rv == JNI_ERR)
    {
      UDT_ThrowExceptionUDT_Message(env, fd,
                                    "can not X_ConvertInetSocketAddressToSockaddr");
      return;
    }

    // connect to the server, implict bind
    if (UDT::ERROR == UDT::connect(fd, (sockaddr *)&remoteSockAddr, sizeof(remoteSockAddr)))
    {
      cout << "connect: " << UDT::getlasterror().getErrorMessage();
      return;
    }
    return;
  }

  jthrowable UDT_NewExceptionUDT( //
      JNIEnv *const env,          //
      const jint socketID,        //
      const jint errorCode,       //
      const char *message         //
  )
  {
    CHK_NUL_RET_NUL(env, "env");
    const jstring messageString = env->NewStringUTF(message);
    CHK_NUL_RET_NUL(messageString, "messageString");
    const jobject exception = env->NewObject(udt_ExceptionUDT,
                                             udt_ExceptionUDT_init0, socketID, errorCode, messageString);
    return static_cast<jthrowable>(exception);
  }

  void UDT_ThrowExceptionUDT_Message( //
      JNIEnv *const env,              //
      const jint socketID,            //
      const char *comment             //
  )
  {
    CHK_NUL_RET_RET(env, "env");
    CHK_NUL_RET_RET(comment, "comment");
    const jint code = UDT_WRAPPER_MESSAGE;
    const jthrowable exception = UDT_NewExceptionUDT(env, socketID, code, comment);
    CHK_NUL_RET_RET(exception, "exception");
    env->Throw(exception);
  }
#ifdef __cplusplus
}
#endif
#endif