/*
 * Copyright 2007-2012 Scott C. Gray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sqsh.renderers;

import java.util.ArrayList;
import java.util.List;

import org.sqsh.ColumnDescription;
import org.sqsh.RendererManager;
import org.sqsh.Session;

/**
 * Implements a renderer that is similar to mysql's or henplus's in that it
 * will attempt to analyze all of the available data to perfectly display
 * all columns without wasting too much space.
 */
public class PerfectPrettyRenderer
    extends AbstractPrettyRenderer {
    
    /*
     * The data that has been collected thus far.
     */
    private List<String[]> rows = new ArrayList<String[]>();
    private int sampleSize = 0;
    private boolean hasHitSampleLimit = false;
    
    /**
     * Creates the renderer.
     * 
     * @param renderMan The owning manager.
     * @param columns The columns. The width specified by the column's
     *    {@link ColumnDescription#getWidth()} will be ignored and calculated
     *    prior to displaying the results.
     */
    public PerfectPrettyRenderer(Session session, RendererManager renderMan) {
        
        super(session, renderMan);
        sampleSize = renderMan.getPerfectSampleSize();
    }
    
    @Override
    public void header (ColumnDescription []columns) {
    
        super.header(columns);
        
        /*
         * We need to throw away the previous result set.
         */
        rows.clear();
        hasHitSampleLimit = false;
        
        for (int i = 0; i < columns.length; i++) {
            
            ColumnDescription col = columns[i];
            if (col.getName() != null) {
                
                col.setWidth(col.getName().length());
            }
            else {
                
                col.setWidth(1);
            }
        }
    }
    
    @Override
    public boolean row (String[] row) {
        
        if (hasHitSampleLimit) {
            
            printRow(row);
        }
        else {
            
            if (sampleSize > 0 && rows.size() >= sampleSize) {
                
                perfectWidth();
                printHeader();
                for (int i = 0; i < rows.size(); i++) {
            
                    if (session.out.checkError() || Thread.interrupted()) {
                
                        return false;
                    }
            
                    printRow(rows.get(i));
                }
                printRow(row);
                
                rows.clear();
                hasHitSampleLimit = true;
            }
            else {
        
                for (int colIdx = 0; colIdx < columns.length; ++colIdx) {
                    
                    ColumnDescription col = columns[colIdx];
                    int width = col.getWidth();
                    
                    if (row[colIdx] == null) {
                        
                        row[colIdx] = session.getDataFormatter().getNull();
                        if (width < row[colIdx].length()) {
                            
                            width = row[colIdx].length();
                        }
                    }
                    else {
                    
                        int lineWidth = getMaxLineWidth(col, row[colIdx]);
                        if (lineWidth > width) {
                            
                            width = lineWidth;
                        }
                    }
                    
                    col.setWidth(width);
                }
            }
            
            rows.add(row);
        }
        
        return true;
    }
    
    /**
     * This method is called just prior to display and attempts to adjust
     * the width of specific columns so that they will try to stay within
     * the width of the screen overall.
     */
    private void perfectWidth() {
        
        /*
         * Attempt to grab the width of the console. If we can't get it,
         * then don't go any further.
         */
        int screenWidth = session.getScreenWidth();
        if (screenWidth <= 0) {
            
            return;
        }
        
        /*
         * Pre-add in the trailing " |" for the closing border.
         */
        int totalWidth = 2;
        for (int i = 0; i < columns.length; i++) {
            
            /*
             * For each column it will be displayed as:
             *    "| value "
             */
            totalWidth += (3 + columns[i].getWidth());
        }
        
        /*
         * This isn't the most efficient way to do things, but we will
         * sit in a loop, shrinking columns until we get the fit that we
         * want. We start by shrinking string columns, but we don't let them
         * fall below a certain size.
         */
        int nShrinks = 1;
        while (nShrinks > 0 && totalWidth > screenWidth) {
            
            nShrinks = 0;
            for (int i = 0; totalWidth > screenWidth
                && i < columns.length; i++) {
                
                ColumnDescription col = columns[i];
                
                if (col.getType() == ColumnDescription.Type.STRING
                        && col.getWidth() > 10) {
                    
                    col.setWidth(col.getWidth() - 1);
                    --totalWidth;
                    ++nShrinks;
                }
            }
        }
        
        /*
         * Move on to number columns.
         */
        nShrinks = 1;
        while (nShrinks > 0 && totalWidth > screenWidth) {
            
            nShrinks = 0;
            for (int i = 0; totalWidth > screenWidth
                && i < columns.length; i++) {
                
                ColumnDescription col = columns[i];
                
                if (col.getType() != ColumnDescription.Type.STRING
                        && col.getWidth() > 4) {
                    
                    col.setWidth(col.getWidth() - 1);
                    --totalWidth;
                    ++nShrinks;
                }
            }
        }
        
    }
    
    @Override
    public boolean flush () {
        
        if (!hasHitSampleLimit) {
            
            perfectWidth();
            
            printHeader();
            for (int i = 0; i < rows.size(); i++) {
                
                if (session.out.checkError() || Thread.interrupted()) {
                    
                    return false;
                }
                
                printRow(rows.get(i));
            }
        }
        printFooter();
        
        return true;
    }
}
