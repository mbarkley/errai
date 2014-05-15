package org.jboss.errai.integration;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

public class SmokeTest {

  private static final String URL = "http://localhost:8080/errai-security-demo";
  private static final String LOGIN_FORM_XPATH = "//form[@data-field='form']";
  private static final int WAIT_TIME_SECONDS = 30;
  private WebDriver driver;

  @Before
  public void setUp() throws Exception {
    driver = new FirefoxDriver();
    driver.manage().timeouts().implicitlyWait(WAIT_TIME_SECONDS, TimeUnit.SECONDS);
    driver.manage().deleteAllCookies();

    driver.get(URL);
  }

  @After
  public void tearDown() throws Exception {
    driver.quit();
  }

  @Test
  public void loginAsJohn() throws Exception {
    navigateToLoginPage();
    login("john", "123", "John");
  }

  @Test
  public void loginAsHacker() throws Exception {
    navigateToLoginPage();
    login("hacker", "123", "Hacker");
  }

  @Test
  public void useAdminServiceAsJohn() throws Exception {
    loginAsJohn();
    navigateToMessagePage();
    pingForAdmin();
    waitForServiceResponse("pong");
  }

  @Test
  public void useAdminServiceAsHacker() throws Exception {
    loginAsHacker();
    navigateToMessagePage();
    pingForAdmin();
    waitForSecurityErrorPage();
  }

  @Test
  public void useHelloServiceAsHacker() throws Exception {
    loginAsHacker();
    navigateToMessagePage();
    useHelloService();
    waitForServiceResponse("Hello Hacker anonymous how are you");
  }

  @Test
  public void useAdminServiceAsNobody() throws Exception {
    navigateToMessagePage();
    pingForAdmin();
    waitForLoginPage();
  }

  @Test
  public void useHelloServiceAsNobody() throws Exception {
    navigateToMessagePage();
    useHelloService();
    waitForLoginPage();
  }

  @Test
  public void navigateToAdminAsNobody() throws Exception {
    navigateToAdminPage();
    waitForLoginPage();
  }

  @Test
  public void navigateToAdminAsHacker() throws Exception {
    loginAsHacker();
    navigateToAdminPage();
    waitForSecurityErrorPage();
  }

  @Test
  public void navigateToAdminAsJohn() throws Exception {
    loginAsJohn();
    navigateToAdminPage();
    waitForText("Welcome to the admin page", By.xpath("//div[@data-field='root']/p"));
  }

  @Test
  public void adminLinkHiddenWhenNotLoggedIn() throws Exception {
    assertAdminLinkIsNotDisplayed();
  }

  @Test
  public void adminLinkHiddenWhenHackerLoggedIn() throws Exception {
    loginAsHacker();
    assertAdminLinkIsNotDisplayed();
  }

  @Test
  public void adminLinkIsShownWhenJohnLoggedIn() throws Exception {
    loginAsJohn();
    assertAdminLinkIsDisplayed();
  }

  private void assertAdminLinkIsNotDisplayed() {
    assertEquals("none", getAdminLink().getCssValue("display"));
  }

  private void assertAdminLinkIsDisplayed() {
    assertFalse("none".equalsIgnoreCase(getAdminLink().getCssValue("display")));
  }

  private WebElement getAdminLink() {
    final By by = By.xpath("//a[@data-field='admin']");
    try {
      return driver.findElement(by);
    }
    catch (NoSuchElementException e) {
      fail("Could not find element: " + by);
      return null;
    }
  }

  private void login(final String username, final String password, final String displayedName) throws InterruptedException {
    driver.findElement(By.xpath(LOGIN_FORM_XPATH + "//input[@data-field='username']")).sendKeys(username);
    driver.findElement(By.xpath(LOGIN_FORM_XPATH + "//input[@data-field='password']")).sendKeys(password);
    driver.findElement(By.xpath(LOGIN_FORM_XPATH + "//button[@data-field='login']")).click();

    waitForText(displayedName, By.xpath("//div[@data-field='userLabel']"));
  }

  private void navigateToLoginPage() throws InterruptedException {
    navigateToPage("login");
    waitForLoginPage();
  }

  private void navigateToMessagePage() throws InterruptedException {
    navigateToPage("messages");
    waitForText("Say Hello!", By.xpath("//button[@data-field='hello']"));
    waitForText("Ping for admin", By.xpath("//button[@data-field='ping']"));
  }

  private void navigateToPage(final String dataField) {
    driver.findElement(By.xpath("//a[@data-field='" + dataField + "']")).click();
  }

  private void navigateToAdminPage() {
    driver.get(URL + "#AdminPage");
  }

  private void pingForAdmin() {
    driver.findElement(By.xpath("//button[@data-field='ping']")).click();
  }

  private void useHelloService() {
    driver.findElement(By.xpath("//button[@data-field='hello']")).click();
  }

  private void waitForServiceResponse(String text) throws InterruptedException {
    waitForText(text, By.xpath("//div[@data-field='newItemForm']"));
  }

  private void waitForLoginPage() throws InterruptedException {
    waitForText("Username", By.xpath(LOGIN_FORM_XPATH + "//label[@for='inputEmail']"));
  }

  private void waitForSecurityErrorPage() throws InterruptedException {
    waitForText("You don't have enough rights access the requested resource.", By.xpath("//div[@data-field='root']/p"));
  }

  private void waitForText(final String text, final By by) throws InterruptedException {
    final long start = System.currentTimeMillis();
    WebElement element = null;

    while (System.currentTimeMillis() - start < WAIT_TIME_SECONDS * 1000) {
      try {
        element = driver.findElement(by);
      }
      catch (NoSuchElementException e) {
        fail("Could not find element: " + by);
      }

      if (text.equals(element.getText())) {
        return;
      }
      Thread.sleep(1000);
    }

    assertEquals(text, element.getText());
  }

}
