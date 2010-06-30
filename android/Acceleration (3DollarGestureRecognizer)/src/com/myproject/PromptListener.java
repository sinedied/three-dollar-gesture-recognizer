package com.myproject;

import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class PromptListener
implements android.content.DialogInterface.OnClickListener{

	
				public String promptReply = "abc";
				public View promptView = null; // set this one publicly
				
				public PromptListener(View promptView)
				{
					this.promptView = promptView;
					
				}

				public void onClick(DialogInterface dialog, int buttonID) {
					// TODO Auto-generated method stub
					
					Log.d("Menu-Click-Listener","selected button: " + buttonID);
					
					if (buttonID == -1)
					{
						// ok button
						promptReply = getPromptText();
						Log.d("Menu-Click-Listener","s changing Text!!!! " + buttonID);

						
					}
					else
					{
						promptReply = null;
					}
				}

				private String getPromptText() {
					// TODO Auto-generated method stub
					EditText et = (EditText) promptView.findViewById(R.id.EditText01);
					return et.getText().toString();
				}
				
				public String getPromptReply() {return promptReply;}
		
			
}
