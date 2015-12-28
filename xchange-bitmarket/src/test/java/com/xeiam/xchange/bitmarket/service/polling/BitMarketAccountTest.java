package com.xeiam.xchange.bitmarket.service.polling;

import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.bitmarket.BitMarketAssert;
import com.xeiam.xchange.bitmarket.BitMarketAuthenticated;
import com.xeiam.xchange.bitmarket.BitMarketExchange;
import com.xeiam.xchange.bitmarket.BitMarketTestSupport;
import com.xeiam.xchange.bitmarket.dto.BitMarketAPILimit;
import com.xeiam.xchange.bitmarket.dto.account.BitMarketAccountInfo;
import com.xeiam.xchange.bitmarket.dto.account.BitMarketAccountInfoResponse;
import com.xeiam.xchange.bitmarket.dto.account.BitMarketBalance;
import com.xeiam.xchange.bitmarket.dto.account.BitMarketDepositResponse;
import com.xeiam.xchange.bitmarket.dto.account.BitMarketWithdrawResponse;
import com.xeiam.xchange.currency.Currency;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.account.Balance;
import com.xeiam.xchange.dto.account.Wallet;
import com.xeiam.xchange.exceptions.ExchangeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import si.mazi.rescu.ParamsDigest;
import si.mazi.rescu.SynchronizedValueFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
public class BitMarketAccountTest extends BitMarketTestSupport {

  private BitMarketAccountService accountService;

  @Before
  public void setUp() {
    BitMarketExchange exchange = (BitMarketExchange) ExchangeFactory.INSTANCE.createExchange(BitMarketExchange.class.getCanonicalName());
    ExchangeSpecification specification = exchange.getExchangeSpecification();
    specification.setUserName(SPECIFICATION_USERNAME);
    specification.setApiKey(SPECIFICATION_API_KEY);
    specification.setSecretKey(SPECIFICATION_SECRET_KEY);

    accountService = new BitMarketAccountService(exchange);
  }

  @Test
  public void constructor() {
    assertThat(Whitebox.getInternalState(accountService, "apiKey")).isEqualTo(SPECIFICATION_API_KEY);
  }

  @Test
  public void shouldGetAccountInfo() throws IOException {
    // given
    final Balance[] expectedBalances = expectedBalances();

    BitMarketAccountInfoResponse response = new BitMarketAccountInfoResponse(
        true,
        new BitMarketAccountInfo(new BitMarketBalance(createAvailable(), createBlocked())),
        new BitMarketAPILimit(3, 100, 12345000L),
        0,
        null
    );

    BitMarketAuthenticated bitMarketAuthenticated = mock(BitMarketAuthenticated.class);
    PowerMockito.when(bitMarketAuthenticated.info(Mockito.eq(SPECIFICATION_API_KEY), Mockito.any(ParamsDigest.class),
        Mockito.any(SynchronizedValueFactory.class))).thenReturn(response);
    Whitebox.setInternalState(accountService, "bitMarketAuthenticated", bitMarketAuthenticated);

    // when
    AccountInfo accountInfo = accountService.getAccountInfo();

    // then
    assertThat(accountInfo.getUsername()).isEqualTo(SPECIFICATION_USERNAME);

    Wallet wallet = accountInfo.getWallet();
    Map<Currency,Balance> balances = wallet.getBalances();

    assertThat(balances).hasSize(3);
    for (int i=0; i<balances.size(); i++) {
      BitMarketAssert.assertEquals(balances.get(expectedBalances[i].getCurrency()), expectedBalances[i]);
    }
  }

  @Test(expected = ExchangeException.class)
  public void shouldFailOnUnsuccessfulGetAccountInfo() throws IOException {
    // given
    BitMarketAccountInfoResponse response = new BitMarketAccountInfoResponse(
        false,
        null,
        null,
        502,
        "Invalid message hash"
    );

    BitMarketAuthenticated bitMarketAuthenticated = mock(BitMarketAuthenticated.class);
    PowerMockito.when(bitMarketAuthenticated.info(Mockito.eq(SPECIFICATION_API_KEY), Mockito.any(ParamsDigest.class),
        Mockito.any(SynchronizedValueFactory.class))).thenReturn(response);
    Whitebox.setInternalState(accountService, "bitMarketAuthenticated", bitMarketAuthenticated);

    // when
    accountService.getAccountInfo();

    // then
    fail("BitMarketAccountService should throw ExchangeException when account info request was unsuccessful");
  }

  @Test
  public void shouldWithdrawFunds() throws IOException {
    // given
    BitMarketWithdrawResponse response = new BitMarketWithdrawResponse(
        true,
        "12345",
        new BitMarketAPILimit(3, 100, 12345000L),
        0,
        null
    );

    BitMarketAuthenticated bitMarketAuthenticated = mock(BitMarketAuthenticated.class);
    PowerMockito.when(bitMarketAuthenticated.withdraw(Mockito.eq(SPECIFICATION_API_KEY), Mockito.any(ParamsDigest.class),
        Mockito.any(SynchronizedValueFactory.class), Mockito.eq(Currency.BTC.toString()),
        Mockito.eq(BigDecimal.TEN), Mockito.eq("address mock")))
        .thenReturn(response);
    Whitebox.setInternalState(accountService, "bitMarketAuthenticated", bitMarketAuthenticated);

    // when
    String withdraw = accountService.withdrawFunds(Currency.BTC, BigDecimal.TEN, "address mock");

    // then
    assertThat(withdraw).isEqualTo("12345");
  }

  @Test(expected = ExchangeException.class)
  public void shouldFailOnUnsuccessfulWithdrawFunds() throws IOException {
    // given
    BitMarketWithdrawResponse response = new BitMarketWithdrawResponse(
        false,
        null,
        null,
        502,
        "Invalid message hash"
    );

    BitMarketAuthenticated bitMarketAuthenticated = mock(BitMarketAuthenticated.class);
    PowerMockito.when(bitMarketAuthenticated.withdraw(Mockito.eq(SPECIFICATION_API_KEY), Mockito.any(ParamsDigest.class),
        Mockito.any(SynchronizedValueFactory.class), Mockito.eq(Currency.BTC.toString()),
        Mockito.eq(BigDecimal.TEN), Mockito.eq("address mock")))
        .thenReturn(response);
    Whitebox.setInternalState(accountService, "bitMarketAuthenticated", bitMarketAuthenticated);

    // when
    accountService.withdrawFunds(Currency.BTC, BigDecimal.TEN, "address mock");

    // then
    fail("BitMarketAccountService should throw ExchangeException when withdraw funds request was unsuccessful");
  }

  @Test
  public void shouldRequestDepositAddress() throws IOException {
    // given
    BitMarketDepositResponse response = new BitMarketDepositResponse(
        true,
        "BITMarket",
        new BitMarketAPILimit(3, 100, 12345000L),
        0,
        null
    );

    BitMarketAuthenticated bitMarketAuthenticated = mock(BitMarketAuthenticated.class);
    PowerMockito.when(bitMarketAuthenticated.deposit(Mockito.eq(SPECIFICATION_API_KEY), Mockito.any(ParamsDigest.class),
        Mockito.any(SynchronizedValueFactory.class), Mockito.eq(Currency.BTC.toString())))
        .thenReturn(response);
    Whitebox.setInternalState(accountService, "bitMarketAuthenticated", bitMarketAuthenticated);

    // when
    String withdraw = accountService.requestDepositAddress(Currency.BTC);

    // then
    assertThat(withdraw).isEqualTo("BITMarket");
  }

  @Test(expected = ExchangeException.class)
  public void shouldFailOnUnsuccessfulRequestDepositAddress() throws IOException {
    // given
    BitMarketDepositResponse response = new BitMarketDepositResponse(
        false,
        null,
        null,
        502,
        "Invalid message hash"
    );

    BitMarketAuthenticated bitMarketAuthenticated = mock(BitMarketAuthenticated.class);
    PowerMockito.when(bitMarketAuthenticated.deposit(Mockito.eq(SPECIFICATION_API_KEY), Mockito.any(ParamsDigest.class),
        Mockito.any(SynchronizedValueFactory.class), Mockito.eq(Currency.BTC.toString())))
        .thenReturn(response);
    Whitebox.setInternalState(accountService, "bitMarketAuthenticated", bitMarketAuthenticated);

    // when
    accountService.requestDepositAddress(Currency.BTC);

    // then
    fail("BitMarketAccountService should throw ExchangeException when deposit funds request was unsuccessful");
  }
}
