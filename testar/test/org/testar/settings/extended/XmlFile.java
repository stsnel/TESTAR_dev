/***************************************************************************************************
 *
 * Copyright (c) 2020 Open Universiteit - www.ou.nl
 * Copyright (c) 2020 Universitat Politecnica de Valencia - www.upv.es
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

package org.testar.settings.extended;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class XmlFile {

    static final String UNKNOWN_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<root>\n" +
            "\t<unkown withAttribute=\"2\">value</unkown>\n" +
            "\t<unkownTag>\n" +
            "\t\t<value>23</value>\n" +
            "\t</unkownTag>\n" +
            "\t<emptyTag/>\n" +
            "</root>";

    public static void CreateUnknownFile(final String absolutePath) {
        CreateFile(absolutePath, UNKNOWN_CONTENT);
    }

    static final String SINGLE_TEST_SETTING_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<root>\n" +
            "\t<unkown withAttribute=\"2\">value</unkown>\n" +
            "\t<unkownTag>\n" +
            "\t\t<value>23</value>\n" +
            "\t</unkownTag>\n" +
            "\t<testSetting>\n" +
            "\t\t<value>Default</value>\n" +
            "\t</testSetting>\n" +
            "\t<emptyTag/>\n" +
            "</root>";

    public static void CreateSingleTestSetting(final String absolutePath) {
        CreateFile(absolutePath, SINGLE_TEST_SETTING_CONTENT);
    }

    static final String MULTIPLE_TEST_SETTING_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<root>\n" +
            "\t<unkown withAttribute=\"2\">value</unkown>\n" +
            "\t<unkownTag>\n" +
            "\t\t<value>23</value>\n" +
            "\t</unkownTag>\n" +
            "\t<testSetting>\n" +
            "\t\t<value>version1</value>\n" +
            "\t</testSetting>\n" +
            "\t<testSetting>\n" +
            "\t\t<value>version2</value>\n" +
            "\t</testSetting>\n" +
            "\t<emptyTag/>\n" +
            "</root>";

    public static void CreateMultipleTestSetting(final String absolutePath) {
        CreateFile(absolutePath, MULTIPLE_TEST_SETTING_CONTENT);
    }

    private static void CreateFile(final String absolutePath, final String content) {
        try {
            FileWriter fileWriter = new FileWriter(absolutePath);
            fileWriter.write(content);
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        File testFile = new File(absolutePath);
        assertTrue(testFile.exists());
    }
}