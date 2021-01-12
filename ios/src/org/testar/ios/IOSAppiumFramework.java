/***************************************************************************************************
 *
 * Copyright (c) 2020 Universitat Politecnica de Valencia - www.upv.es
 * Copyright (c) 2020 Open Universiteit - www.ou.nl
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************************************/

package org.testar.ios;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.fruit.alayer.AutomationCache;
import org.fruit.alayer.SUT;
import org.fruit.alayer.SUTBase;
import org.fruit.alayer.exceptions.SystemStopException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.appium.java_client.MobileElement;
import io.appium.java_client.ios.IOSDriver;

public class IOSAppiumFramework extends SUTBase {

	public static IOSAppiumFramework iosSUT = null;

	private static IOSDriver<MobileElement> driver = null;

	public IOSAppiumFramework(DesiredCapabilities cap) {

		try {
			driver = new IOSDriver<>(new URL("http://127.0.0.1:4723/wd/hub"), cap);
		} catch (MalformedURLException e) {
			System.out.println("ERROR: Exception with IOS Driver URL: http://127.0.0.1:4723/wd/hub");
			e.printStackTrace();
		}
	}

	public static IOSAppiumFramework fromCapabilities(String capabilitesJsonFile) {
		if (iosSUT != null) {
			iosSUT.stop();
		}

		DesiredCapabilities cap = createCapabilitiesFromJsonFile(capabilitesJsonFile);

		return new IOSAppiumFramework(cap);
	}

	public static List<MobileElement> findElements(By by){
		return driver.findElements(by);
	}
	
	// Send Click Action
	
	public static void clickElementById(String id){
		driver.findElementById(id).click();
	}
	
	// Send Type Action
	
	public static void setValueElementById(String id, String value){
		driver.findElementById(id).setValue(value);
	}
	
	public static void sendKeysElementById(String id, CharSequence keysToSend){
		driver.findElementById(id).sendKeys(keysToSend);
	}
	
    // TODO: Complete for IOSDriver, KeyEvent seems android specific?
	/*public static void pressKeyEvent(KeyEvent keyEvent){
		driver.pressKey(keyEvent);
	}*/
	
	// Utility Interactions
	
	public static void hideKeyboard(){
		driver.hideKeyboard();
	}
	
	// TODO: Complete for IOSDriver, KeyEvent seems android specific?
	/*public static void wakeUpKeyCode(){
		driver.pressKey(new KeyEvent(AndroidKey.WAKEUP));
	}*/
	
	public static void activateAppByBundleId(String bundleId){
		driver.activateApp(bundleId);
	}
	
	public static List<Map<String, Object>> getAllSessionDetails(){
		return driver.getAllSessionDetails();
	}
	
	public static Set<String> getWindowHandles(){
		return driver.getWindowHandles();
	}
	
	public static String getTitleOfCurrentPage(){
		return driver.getTitle();
	}
	
	public static void resetApp(){
		driver.resetApp();
	}
	
	public static void runAppInBackground(Duration duration){
		driver.runAppInBackground(duration);
	}
	
	public static void pushFile(String remotePath, File file){
		try {
			driver.pushFile(remotePath, file);
		} catch (IOException e) {
			System.out.println("Exception: IOSDriver pushFile request was not properly executed");
		}
	}
	
	public static void terminateApp(String bundleId){
		driver.terminateApp(bundleId);
	}

	/**
	 * Obtain a Document representation of the IOS loaded page DOM.
	 * 
	 * Information is about loaded page/application in the foreground,
	 * not about specific process or SUT.
	 * 
	 * @return Document with DOM representation
	 */
	public static Document getIOSPageSource() {
		try {
			return loadXML(driver.getPageSource());
		} catch (WebDriverException wde) {
			System.out.println("ERROR: Exception trying to obtain driver.getPageSource()");
		} catch (ParserConfigurationException | SAXException | IOException doce) {
			System.out.println("ERROR: Exception parsing IOS Driver Page Source to XML Document");
		} catch (Exception e) {
			System.out.println("ERROR: Unknown Exception AppiumFramework getIOSPageSource()");
			e.printStackTrace();
		}
		return null;
	}

	private static Document loadXML(String xml) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory fctr = DocumentBuilderFactory.newInstance();
		DocumentBuilder bldr = fctr.newDocumentBuilder();
		InputSource insrc = new InputSource(new StringReader(xml));
		return bldr.parse(insrc);
	}

	@Override
	public void stop() throws SystemStopException {
		driver.closeApp();
		driver = null;
	}

	@Override
	public boolean isRunning() {
		//TODO: Check and select proper method to verify if running
		try {
		    driver.getPageSource();
		}
		catch (Exception e) {
			return false;
		}

		return true;
	}

	@Override
	public String getStatus() {
		//TODO: Check and select proper method to print the status
		return "IOS Page Source : " + driver.getPageSource();
	}

	@Override
	public AutomationCache getNativeAutomationCache() {
		return null;
	}

	@Override
	public void setNativeAutomationCache() {
	}

	public static List<SUT> fromAll() {
		if (iosSUT == null) {
			return new ArrayList<>();
		}

		return Collections.singletonList(iosSUT);
	}

	private static DesiredCapabilities createCapabilitiesFromJsonFile(String capabilitesJsonFile) {
		DesiredCapabilities cap = new DesiredCapabilities();

		try (FileReader reader = new FileReader(capabilitesJsonFile)) {

			JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();

			cap.setCapability("deviceName", jsonObject.get("deviceName").getAsString());
			cap.setCapability("platformName", jsonObject.get("platformName").getAsString());

			String appPath = jsonObject.get("app").getAsString();

			cap.setCapability("app", new File(appPath).getCanonicalPath());

		} catch (IOException | NullPointerException e) {
			System.out.println("ERROR: Exception reading Appium Desired Capabilities from JSON file: " + capabilitesJsonFile);
			e.printStackTrace();
		}

		return cap;
	}

}
