/*  Copyright 2013 Handpoint

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.handpoint.headstart.client.util;
import android.text.Editable;
import android.text.Html.TagHandler;
import org.xml.sax.XMLReader;



public class ReceiptTagHandler implements TagHandler {

    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
        //Todo ... handle table, td, tr,
        //System.out.println("Unsupported tag detected");
        //System.out.println(tag);
        if (tag.equalsIgnoreCase("table")) {
            processTable(opening,output);
        }

        if (tag.equalsIgnoreCase("tr")) {
            processTableRow(opening,output);
        }

        if (tag.equalsIgnoreCase("td")) {
            processTableData(opening,output);
        }


    }

    private void processTable(boolean opening, Editable output) {
        if(!opening) {
            output.append("\n");
        }

    }

    private void processTableRow(boolean opening, Editable output) {
        if(!opening) {
            output.append("\n");
        }

    }


    private void processTableData(boolean opening, Editable output) {
        if(!opening) {
            output.append("\u0020\u0020");
        }

    }

}
