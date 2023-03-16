/**
 * Copyright (c) 2019 - 2023 Open Universiteit - www.ou.nl
 * Copyright (c) 2019 - 2023 Universitat Politecnica de Valencia - www.upv.es
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
 *
 */

package org.testar.monkey.alayer.actions;

import org.testar.monkey.alayer.Role;

public class WdActionRoles {
	private WdActionRoles(){}

	public static final Role

	ExecuteScript = Role.from("ExecuteScript", ActionRoles.Action), 
	CloseTabScript = Role.from("CloseTabScript", ExecuteScript),
	HistoryBackScript = Role.from("HistoryBackScript", ExecuteScript),
	SubmitScript = Role.from("SubmitScript", ExecuteScript),
	SetAttributeScript = Role.from("SetAttributeScript", ExecuteScript),
	FormFillingAction = Role.from("FormFillingAction", ActionRoles.CompoundAction),
	SelectListAction = Role.from("SelectListAction", ExecuteScript),
	RemoteAction = Role.from("RemoteAction", ActionRoles.Action),
	RemoteClick = Role.from("RemoteClick", WdActionRoles.RemoteAction),
	RemoteScrollClick = Role.from("RemoteScrollClick", WdActionRoles.RemoteClick),
	RemoteType = Role.from("RemoteType", WdActionRoles.RemoteAction),
	RemoteScrollType = Role.from("RemoteScrollType", WdActionRoles.RemoteType);

}
