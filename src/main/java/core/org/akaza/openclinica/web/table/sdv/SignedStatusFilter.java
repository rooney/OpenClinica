package core.org.akaza.openclinica.web.table.sdv;

import org.jmesa.view.html.editor.DroplistFilterEditor;

import java.util.ArrayList;
import java.util.List;

public class SignedStatusFilter extends DroplistFilterEditor {
    @Override
    protected List<Option> getOptions() {
        List<Option> options = new ArrayList<Option>();
        options.add(new Option("Signed", "Signed"));
        options.add(new Option("Not signed","Not signed"));
        return options;
    }
}