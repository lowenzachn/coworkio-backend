package com.coworkio.service.security

import com.coworkio.dto.UserDto
import com.coworkio.dto.mapper.UserDtoMapper
import com.coworkio.entity.domain.User
import com.coworkio.service.domain.UserService
import com.coworkio.util.security.TokenBuilder
import com.coworkio.util.security.TokenParser
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.nio.charset.Charset
import java.util.*

@Service
@Qualifier("authenticationService")
open class AuthenticationService {

    private val UTF8 = "UTF-8"
    private val log = LogFactory.getLog(this.javaClass)

    @Autowired
    private lateinit var tokenBuilder: TokenBuilder

    @Autowired
    private lateinit var tokenParser: TokenParser

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    fun login(email: String, password: String): Authentication {
        val user = userService.findByEmail(email)

        return if (passwordEncoder.matches(password, user?.password)) {
            UsernamePasswordAuthenticationToken(email, null)
        } else {
            throw BadCredentialsException("Username and password are not match.")
        }
    }

    fun register(user: UserDto) {
        if(userService.exists(user)) {
            throw BadCredentialsException("provided user already exists")
        }
        val userDomain = UserDtoMapper().toDomain(user)
        userService.saveOrUpdate(userDomain.copy(password = encodePassword(userDomain.password)))
    }

    fun confirm(encodedToken: String): Boolean {
        val token = String(Base64.getDecoder().decode(encodedToken), Charset.forName(UTF8))

        try {
            val username = tokenParser.getConfirmationFromToken(token)
            //TODO: update account_confirmed field
        } catch (ex: BadCredentialsException) {
            log.error("Unable to confirm registration.", ex)
            return false
        }

        return true
    }

    open fun encodePassword(password: String):String
            = passwordEncoder.encode(password)
}