/*
 * Copyright 2014-2018 Robin Stuart, Daniel Gredler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.okapibarcode.backend;

/**
 * A simple text item class.
 *
 * @author <a href="mailto:rstuart114@gmail.com">Robin Stuart</a>
 * @author Daniel Gredler
 */
public class TextBox {

    /** The X position of the text's left boundary. */
    public final double x;

    /** The Y position of the text baseline. */
    public final double y;

    /** The width of the text box. */
    public final double width;

    /** The text value. */
    public final String text;

    /** The text alignment. */
    public final HumanReadableAlignment alignment;

    /**
     * Creates a new instance.
     *
     * @param x the X position of the text's left boundary
     * @param y the Y position of the text baseline
     * @param width the width of the text box
     * @param text the text value
     * @param alignment the text alignment
     */
    public TextBox(double x, double y, double width, String text, HumanReadableAlignment alignment) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.text = text;
        this.alignment = alignment;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "TextBox[x=" + x + ", y=" + y + ", width=" + width + ", text=" + text + ", alignment=" + alignment + "]";
    }
}
