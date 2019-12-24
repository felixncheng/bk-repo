package com.tencent.bkrepo.auth.resource

import com.tencent.bkrepo.auth.api.ServiceAccountResource
import com.tencent.bkrepo.auth.pojo.Account
import com.tencent.bkrepo.auth.pojo.CreateAccountRequest
import com.tencent.bkrepo.auth.pojo.CredentialSet
import com.tencent.bkrepo.auth.pojo.enums.CredentialStatus
import com.tencent.bkrepo.auth.service.AccountService
import com.tencent.bkrepo.common.api.pojo.Response
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceAccountResourceImpl @Autowired constructor(
    private val accountService: AccountService
) : ServiceAccountResource {

    override fun listAccount(): Response<List<Account>> {
        val accountList = accountService.listAccount()
        return Response(accountList)
    }

    override fun createAccount(request: CreateAccountRequest): Response<Account?> {
        return Response(accountService.createAccount(request))
    }

    override fun updateAccount(appid: String, locked: Boolean): Response<Boolean> {
        accountService.updateAccountStatus(appid, locked)
        return Response(true)
    }

    override fun deleteAccount(appid: String): Response<Boolean> {
        accountService.deleteAccount(appid)
        return Response(true)
    }

    override fun getCredential(account: String): Response<List<CredentialSet>> {
        val credential = accountService.listCredentials(account)
        return Response(credential)
    }

    override fun createCredential(appid: String): Response<List<CredentialSet>> {
        val result = accountService.createCredential(appid)
        return Response(result)
    }


    override fun deleteCredential(appid: String, accesskey: String): Response<List<CredentialSet>> {
        val result = accountService.deleteCredential(appid, accesskey)
        return Response(result)
    }

    override fun updateCredential(appid: String, accesskey: String, status: CredentialStatus): Response<Boolean> {
        accountService.updateCredentialStatus(appid, accesskey, status)
        return Response(true)
    }

    override fun checkCredential(accesskey: String, secretkey: String): Response<String> {
        val result = accountService.checkCredential(accesskey, secretkey)
        return Response(result)
    }
}