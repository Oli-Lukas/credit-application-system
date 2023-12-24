package me.dio.credit.application.system.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.dio.credit.application.system.dto.request.CreditDto
import me.dio.credit.application.system.dto.request.CustomerDto
import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.enummeration.Status
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.repository.CustomerRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration
class CreditResourceTest {
    @Autowired private lateinit var customerRepository: CustomerRepository
    @Autowired private lateinit var creditRepository: CreditRepository
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    companion object {
        const val URL: String = "/api/credits"
    }

    @Test
    fun `should create a credit and return 201 status`() {
        val fakeCustomerId = 1L
        val fakeCustomer = buildCustomer(id = fakeCustomerId)
        customerRepository.save(fakeCustomer)

        val creditDto: CreditDto = buildCreditDto()
        val valueAsString = objectMapper.writeValueAsString(creditDto)

        mockMvc
            .perform(MockMvcRequestBuilders.post(URL).contentType(MediaType.APPLICATION_JSON).content(valueAsString))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should find all credits by customer id and return 200 status`() {
        val fakeCustomerId: Long = 1L
        val fakeCustomer: Customer = buildCustomer(id = fakeCustomerId)

        customerRepository.save(fakeCustomer)
        creditRepository.save(buildCredit(
            id = 1L,
            creditValue = BigDecimal.valueOf(1500.0),
            dayFirstInstallment = LocalDate.now().plusDays(10),
            customer =  fakeCustomer
        ))
        creditRepository.save(buildCredit(
            id = 2L,
            creditValue = BigDecimal.valueOf(2000.0),
            dayFirstInstallment = LocalDate.now().plusDays(15),
            customer = fakeCustomer
        ))

        mockMvc
            .perform(MockMvcRequestBuilders.get("$URL?customerId=${fakeCustomerId}").contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].creditCode").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].creditValue").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].numberOfInstallments").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].creditCode").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].creditValue").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].numberOfInstallments").value(1))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should find a credit by credit code and customer Id and return 200 status`() {
        val fakeCustomerId = 1L
        val fakeCustomer = buildCustomer(id = fakeCustomerId)
        val fakeCredit = buildCredit(
            creditValue = BigDecimal.valueOf(1200.00),
            dayFirstInstallment = LocalDate.now().plusMonths(1L),
            customer = fakeCustomer
        )
        val fakeCreditCode: UUID = fakeCredit.creditCode

        customerRepository.save(fakeCustomer)
        creditRepository.save(fakeCredit)

        mockMvc
            .perform(MockMvcRequestBuilders.get("$URL/${fakeCreditCode.toString()}?customerId=${fakeCustomerId}").contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditCode").value(fakeCreditCode.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value(fakeCredit.creditValue))
            .andExpect(MockMvcResultMatchers.jsonPath("$.numberOfInstallment").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.emailCustomer").value(fakeCustomer.email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.incomeCustomer").value(fakeCustomer.income))
            .andDo(MockMvcResultHandlers.print())
    }

    private fun buildCreditDto(
        creditValue: BigDecimal = BigDecimal.valueOf(1500.0),
        dayFirstOfInstallment: LocalDate = LocalDate.now().plusDays(10),
        numberOfInstallments: Int = 1,
        customerId: Long = 1L
    ): CreditDto = CreditDto(
        creditValue = creditValue,
        dayFirstOfInstallment = dayFirstOfInstallment,
        numberOfInstallments = numberOfInstallments,
        customerId = customerId
    )

    private fun buildCredit(
        creditCode: UUID = UUID.randomUUID(),
        creditValue: BigDecimal = BigDecimal.ZERO,
        dayFirstInstallment: LocalDate = LocalDate.now(),
        numberOfInstallments: Int = 1,
        status: Status = Status.IN_PROGRESS,
        customer: Customer = buildCustomer(),
        id: Long = 1L
    ) = Credit(
        creditCode = creditCode,
        creditValue = creditValue,
        dayFirstInstallment = dayFirstInstallment,
        numberOfInstallments = numberOfInstallments,
        status = status,
        customer = customer,
        id = id
    )

    private fun buildCustomer(
        firstName: String = "Vitor",
        lastName: String = "Silveira",
        cpf: String = "11111111111",
        email: String = "vitor.silveira@gmail.com",
        password: String = "12345",
        zipCode: String = "58802118",
        street: String = "Rua Pedro Celestino de Paula",
        income: BigDecimal = BigDecimal.valueOf(1000.0),
        id: Long = 1L
    ) = Customer(
        firstName = firstName,
        lastName = lastName,
        cpf = cpf,
        email = email,
        password = password,
        address = Address(
            zipCode = zipCode,
            street = street
        ),
        income = income,
        id = id
    )
}