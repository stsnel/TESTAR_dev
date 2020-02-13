package nl.ou.testar.temporal.structure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvBindAndSplitByName;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;
import es.upv.staq.testar.StateManagementTags;
import org.fruit.alayer.Tag;
import org.fruit.alayer.windows.UIAMapping;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class TemporalMeta  {
protected static final String csvsep=";";

    @CsvBindAndSplitByName(elementType = String.class, splitOn = csvsep+"+", writeDelimiter = csvsep)//, collectionType = LinkedList.class)
    private List<String> _log;
    @CsvBindAndSplitByName(elementType = String.class, splitOn = csvsep+"+", writeDelimiter = csvsep)//, collectionType = LinkedList.class)
    private List<String> _comments;
    @CsvBindByName
    private String _modifieddate;



    public TemporalMeta() {
        _log = new ArrayList<String>();
        _comments = new ArrayList<String>();
        _modifieddate = LocalDateTime.now().toString();
    }


    public void addLog(String log) {
        this._log.add(log);
    }


    public List<String> get_log() {
        return _log;
    }


    public void set_log(List<String> _log) {
        this._log = _log;
    }

    public List<String> get_comments() {
        return _comments;
    }


    public void set_comments(List<String> _comments) {
        this._comments = _comments;
    }

    public void addComments(String comment) {
        this._comments.add(comment);
    }


    public String get_modifieddate() {
        return _modifieddate;
    }


    public void set_modifieddate(String _modifieddate) {
        this._modifieddate = _modifieddate;
    }


}
