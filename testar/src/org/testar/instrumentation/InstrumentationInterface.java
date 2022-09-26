package org.testar.instrumentation;

public interface InstrumentationInterface {

    public void setLogContext(String newContext);

    public void setCoverageContext (String newContext);

    public StringBuffer extractStrings(String context);

    public StringBuffer clearLogDataExportCoverage();

    public StringBuffer exportCoverage();

    public void importCoverage(StringBuffer data);
}
