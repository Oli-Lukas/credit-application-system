package me.dio.credit.application.system.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.enummeration.Status
import me.dio.credit.application.system.exception.BusinessException
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.service.impl.CreditService
import me.dio.credit.application.system.service.impl.CustomerService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Random
import java.util.UUID

@ExtendWith(MockKExtension::class)
class CreditServiceTest {
    @MockK lateinit var creditRepository: CreditRepository
    @MockK lateinit var customerService: CustomerService

    @InjectMockKs lateinit var creditService: CreditService

    @Test
    fun `should save a credit`() {
        val fakeCustomerId: Long = Random().nextLong()
        val fakeCustomer: Customer = buildCustomer(id = fakeCustomerId)
        val fakeCredit: Credit = buildCredit(customer = fakeCustomer)

        every { creditRepository.save(fakeCredit) } returns fakeCredit
        every { customerService.findById(fakeCustomerId) } returns fakeCustomer

        val actual: Credit = creditService.save(fakeCredit)

        Assertions.assertThat(actual).isNotNull
        Assertions.assertThat(actual).isSameAs(fakeCredit)

        verify(exactly = 1) { creditRepository.save((fakeCredit)) }
        verify(exactly = 1) { customerService.findById(fakeCustomerId) }
    }

    @Test
    fun `should not save a credit by invalid day first of installment`() {
        val fakeInvalidDayFirstOfInstallment = LocalDate.now().plusMonths(3)

        val fakeCustomerId: Long = Random().nextLong()
        val fakeCustomer: Customer = buildCustomer(id = fakeCustomerId)
        val fakeCredit: Credit = buildCredit(customer = fakeCustomer, dayFirstInstallment = fakeInvalidDayFirstOfInstallment)

        Assertions.assertThatExceptionOfType(BusinessException::class.java)
            .isThrownBy { creditService.save(fakeCredit) }
            .withMessage("Invalid Date")
    }

    @Test
    fun `should get all credits from a given customer`() {
        val fakeCustomerId = Random().nextLong()
        val fakeCustomer   = buildCustomer(id = fakeCustomerId)

        val fakeCredits: List<Credit> = listOf<Credit>(
            buildCredit(customer = fakeCustomer, creditValue = BigDecimal.valueOf(1500.00)),
            buildCredit(customer = fakeCustomer, creditValue = BigDecimal.valueOf(2000.00)),
            buildCredit(customer = fakeCustomer, creditValue = BigDecimal.valueOf(3000.00))
        )
        every { creditService.findAllByCustomer(any()) } returns fakeCredits

        val actual: List<Credit> = creditService.findAllByCustomer(fakeCustomerId)

        Assertions.assertThat(actual).isNotNull
        Assertions.assertThat(actual).isNotEmpty
        Assertions.assertThat(actual).isSameAs(fakeCredits)

        verify(exactly = 1) { creditRepository.findAllByCustomerId(fakeCustomerId) }
    }

    @Test
    fun `should find credit by specifying credit code`() {
        val fakeCustomerId = Random().nextLong()
        val fakeCustomer = buildCustomer(id = fakeCustomerId)

        val fakeCreditCode: UUID = UUID.randomUUID()
        val fakeCredit = buildCredit(customer = fakeCustomer, creditCode = fakeCreditCode)
        every { creditRepository.findByCreditCode(any()) } returns fakeCredit

        val actual: Credit = creditService.findByCreditCode(fakeCustomerId, fakeCreditCode)

        Assertions.assertThat(actual).isNotNull
        Assertions.assertThat(actual).isExactlyInstanceOf(Credit::class.java)
        Assertions.assertThat(actual.creditCode).isEqualTo(fakeCreditCode)

        verify(exactly = 1) { creditRepository.findByCreditCode(fakeCreditCode) }
    }

    @Test
    fun `should not find credit by incorrect credit code`() {
        val fakeCustomerId = Random().nextLong()
        val fakeIncorrectCreditCode: UUID = UUID.randomUUID()

        every { creditRepository.findByCreditCode(fakeIncorrectCreditCode) } returns null

        Assertions.assertThatExceptionOfType(BusinessException::class.java)
            .isThrownBy { creditService.findByCreditCode(fakeCustomerId, fakeIncorrectCreditCode) }
            .withMessage("Creditcode $fakeIncorrectCreditCode not found")

        verify(exactly = 1) { creditRepository.findByCreditCode(fakeIncorrectCreditCode) }
    }

    @Test
    fun `should not find credit by incorrect customer id`() {
        val fakeCorrectCustomerId = Random().nextLong()
        val fakeIncorrectCustomerId = generateDifferentCustomerId(fakeCorrectCustomerId)
        val fakeCreditCode: UUID = UUID.randomUUID()

        val fakeCustomer: Customer = buildCustomer(id = fakeCorrectCustomerId)
        val fakeCredit: Credit = buildCredit(customer = fakeCustomer, creditCode = fakeCreditCode)

        every { creditRepository.findByCreditCode(fakeCreditCode) } returns fakeCredit

        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { creditService.findByCreditCode(fakeIncorrectCustomerId, fakeCreditCode) }
            .withMessage("Contact admin")

        verify(exactly = 1) { creditRepository.findByCreditCode(fakeCreditCode) }
    }

    private fun generateDifferentCustomerId(actualCustomerId: Long): Long {

        var differentCustomerId: Long = Random().nextLong()

        while (differentCustomerId == actualCustomerId)
            differentCustomerId = Random().nextLong()

        return differentCustomerId
    }

    private fun buildCredit(
        creditCode: UUID = UUID.randomUUID(),
        creditValue: BigDecimal = BigDecimal.ZERO,
        dayFirstInstallment: LocalDate = LocalDate.now(),
        numberOfInstallments: Int = 0,
        status: Status = Status.IN_PROGRESS,
        customer: Customer? = buildCustomer(),
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