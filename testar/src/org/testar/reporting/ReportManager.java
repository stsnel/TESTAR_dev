package org.testar.reporting;

import org.testar.OutputStructure;
import org.testar.monkey.ConfigTags;
import org.testar.monkey.alayer.Action;
import org.testar.monkey.alayer.State;
import org.testar.monkey.alayer.Tags;
import org.testar.monkey.alayer.Verdict;
import org.testar.settings.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class ReportManager implements Reporting
{
    private ArrayList<Reporting> reporters;
    private boolean reportingEnabled = true;
    private boolean firstStateAdded = false;
    private boolean firstActionsAdded = false;
    
    public ReportManager(boolean replay, Settings settings)
    {
        //TODO put filename into settings, name with sequence number
        // creating a new file for the report
        String fileName =
                OutputStructure.htmlOutputDir + "/" + OutputStructure.startInnerLoopDateString + "_"
                + OutputStructure.executedSUTname + "_sequence_" + OutputStructure.sequenceInnerLoopCount; //no File.separator
        
        boolean html = settings.get(ConfigTags.ReportInHTML);
        boolean plainText = settings.get(ConfigTags.ReportInPlainText);
        
        if(!Arrays.asList(html, plainText).contains(Boolean.TRUE)) //if none of the options are true
        {
            reportingEnabled = false;
        }
        else
        {
            reporters = new ArrayList<>();
    
            if(html)
                reporters.add(new HTMLreporter(fileName, replay));
            if(plainText)
                reporters.add(new PlainTextReporter(fileName, replay));
        }
    }
    
    public void finishReport()
    {
        if(reportingEnabled)
            for(Reporting reporter : reporters)
                reporter.finishReport();
    }
    
    public void addState(State state)
    {
        if(reportingEnabled)
        {
            if(firstStateAdded)
            {
                if(firstActionsAdded || (state.get(Tags.OracleVerdict, Verdict.OK).severity() > Verdict.SEVERITY_OK))
                { //if the first state contains a failure, write the same state in case it was a login
                    for(Reporting reporter : reporters)
                        reporter.addState(state);
                } //no else branch: don't write the state as it is the same - getState is run twice in the beginning, before the first action
            }
            else
            {
                firstStateAdded = true;
                for(Reporting reporter : reporters)
                    reporter.addState(state);
            }
        }
    }
    
    public void addActions(Set<Action> actions)
    {
        if(reportingEnabled)
        {
            firstActionsAdded = true;
    
            for(Reporting reporter : reporters)
                reporter.addActions(actions);
        }
    }
    
    public void addActionsAndUnvisitedActions(Set<Action> actions, Set<String> concreteIdsOfUnvisitedActions)
    {
        if(reportingEnabled)
        {
            firstActionsAdded = true;
    
            for(Reporting reporter : reporters)
                reporter.addActionsAndUnvisitedActions(actions, concreteIdsOfUnvisitedActions);
        }
    }
    
    public void addSelectedAction(State state, Action action)
    {
        if(reportingEnabled)
            for(Reporting reporter : reporters)
                reporter.addSelectedAction(state, action);
    }
    
    public void addTestVerdict(Verdict verdict)
    {
        if(reportingEnabled)
            for(Reporting reporter : reporters)
                reporter.addTestVerdict(verdict);
    }
}
