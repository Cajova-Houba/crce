package cz.zcu.kiv.crce.crce_webui_vaadin;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;

@SuppressWarnings("serial")
@StyleSheet("https://fonts.googleapis.com/css?family=Fredoka+One")
public class MenuForm extends FormLayout{
	public MenuForm(MyUI myUI) {
		HorizontalLayout headHL = new HorizontalLayout();
		Label logo = new Label("<p style=\"font-size:50px;font-family:'Fredoka One';padding-left:10px;"
				+ "margin:0px 0px 0px 0px;color:rgb(23,116,216)\">CRCE UI</p>",ContentMode.HTML);
		MenuBar menu = new MenuBar();
		MenuItem upload = menu.addItem("Upload", null);
		MenuItem repository = menu.addItem("Repository", null);
		MenuItem settings = menu.addItem("Settings", null);
		
		// submenu upload
		upload.addItem("Local", e ->{myUI.setContentBodyLocalMaven();});
		upload.addItem("Central", e ->{myUI.setContentBodyCentralMaven();});
		upload.addItem("Defined", e ->{myUI.setContentBodyDefinedMaven();});
		upload.addItem("File", e ->{myUI.setContentBodyLoadFile();});
		
		// submenu repository
		repository.addItem("Refresh", null);
		
		// submenu settings
		settings.addItem("Repository", e ->{myUI.setContentSettings();});
		
		headHL.addComponents(logo,menu);
		headHL.setComponentAlignment(menu,Alignment.MIDDLE_LEFT);
		headHL.setSpacing(true);
		headHL.setMargin(false);
		addComponent(headHL);
	}
}
