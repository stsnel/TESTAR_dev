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

package org.testar.android;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.Callable;

import org.fruit.alayer.Rect;
import org.fruit.alayer.Roles;
import org.fruit.alayer.SUT;
import org.fruit.alayer.Tags;
import org.fruit.alayer.exceptions.StateBuildException;
import org.fruit.alayer.windows.Windows;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class AndroidStateFetcher implements Callable<AndroidState> {

	private final SUT system;

	public AndroidStateFetcher(SUT system) {
		this.system = system;
	}

	public static AndroidRootElement buildRoot(SUT system) throws StateBuildException {
		AndroidRootElement Androidroot = new AndroidRootElement();
		Androidroot.isRunning = system.isRunning();
		Androidroot.timeStamp = System.currentTimeMillis();
		Androidroot.pid = (long)-1; //TODO: Emulator pid + android pid
		Androidroot.isForeground = false; //TODO: Windows Emulator + android process

		return Androidroot;
	}

	@Override
	public AndroidState call() throws Exception {
		AndroidRootElement rootElement = buildAndroidSkeleton(system);

		if (rootElement == null) {
			system.set(Tags.Desc, " ");
			return new AndroidState(null);
		}

		system.set(Tags.Desc, "Android system");

		AndroidState root = createWidgetTree(rootElement);
		root.set(Tags.Role, Roles.Process);
		root.set(Tags.NotResponding, false);

		return root;
	}

	private AndroidRootElement buildAndroidSkeleton(SUT system) {
		AndroidRootElement rootElement = buildRoot(system);
		
		if(!rootElement.isRunning)
			return rootElement;
		
		rootElement.pid = system.get(Tags.PID, (long)-1);
		
		Document xmlAndroid = AppiumFramework.getAndroidPageSource();
		
		Node stateNode = xmlAndroid.getDocumentElement();
		System.out.println("Android XML State Node: " + stateNode.toString());
		
		rootElement.ignore = false;
		rootElement.enabled = true;
		rootElement.blocked = false;
		rootElement.zindex = 0;
		
		if(stateNode.hasChildNodes()) {
			int childNum = stateNode.getChildNodes().getLength();
			rootElement.children = new ArrayList<AndroidElement>(childNum);
			
			for(int i = 0; i < childNum; i++) {
				XmlNodeDescend(rootElement, stateNode.getChildNodes().item(i));
			}
		}
		
		return rootElement;
	}
	
	private void XmlNodeDescend(AndroidElement parent, Node xmlNode) {
		AndroidElement childElement = new AndroidElement(parent);
		parent.children.add(childElement);
		
		childElement.ignore = false;
		childElement.enabled = true;
		childElement.blocked = false;
		
		childElement.zindex = parent.zindex + 1;
		
		System.out.println("Child XML... " + xmlNode.getNodeName() + ", Attributes:");
		if(xmlNode.getAttributes() != null) {
			for(int a = 0; a < xmlNode.getAttributes().getLength(); a++) {
				Node attribute = xmlNode.getAttributes().item(a);
				System.out.println("Name: " + attribute.getNodeName() + ", Value: " + attribute.getNodeValue());
			}
		}
		
		if(xmlNode.hasChildNodes()) {
			int childNum = xmlNode.getChildNodes().getLength();
			childElement.children = new ArrayList<AndroidElement>(childNum);
			
			for(int i = 0; i < childNum; i++) {
				XmlNodeDescend(childElement, xmlNode.getChildNodes().item(i));
			}
		}
	}

	private AndroidState createWidgetTree(AndroidRootElement root) {
		AndroidState state = new AndroidState(root);
		root.backRef = state;
		for (AndroidElement childElement : root.children) {
			if (!childElement.ignore) {
				createWidgetTree(state, childElement);
			}
		}
		return state;
	}

	private void createWidgetTree(AndroidWidget parent, AndroidElement element) {
		if (!element.enabled) {
			return;
		}

		AndroidWidget w = parent.root().addChild(parent, element);
		element.backRef = w;

		for (AndroidElement child : element.children) {
			createWidgetTree(w, child);
		}
	}

	/* lists all visible top level windows in ascending z-order (foreground window last) */
	private Iterable<Long> getVisibleTopLevelWindowHandles(){
		Deque<Long> ret = new ArrayDeque<Long>();
		long windowHandle = Windows.GetWindow(Windows.GetDesktopWindow(), Windows.GW_CHILD);

		while(windowHandle != 0){
			if(Windows.IsWindowVisible(windowHandle)){
				long exStyle = Windows.GetWindowLong(windowHandle, Windows.GWL_EXSTYLE);
				if((exStyle & Windows.WS_EX_TRANSPARENT) == 0 && (exStyle & Windows.WS_EX_NOACTIVATE) == 0){
					ret.addFirst(windowHandle);
				}				
			}
			windowHandle = Windows.GetNextWindow(windowHandle, Windows.GW_HWNDNEXT);
		}

		System.clearProperty("DEBUG_WINDOWS_PROCESS_NAMES");

		return ret;
	}

	private Rect windowsEmulatorRect(long windowsHandle) {
		long r[] = Windows.GetWindowRect(windowsHandle);
		if(r[2] - r[0] >= 0 && r[3] - r[1] >= 0) {
			return Rect.fromCoordinates(r[0], r[1], r[2], r[3]);
		}

		return null;
	}

}
