/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.auraframework.renderer;

import java.io.IOException;
import java.util.List;

import org.auraframework.Aura;
import org.auraframework.def.ComponentDefRef;
import org.auraframework.def.Renderer;
import org.auraframework.instance.BaseComponent;
import org.auraframework.instance.Component;
import org.auraframework.instance.Wrapper;
import org.auraframework.service.RenderingService;
import org.auraframework.throwable.quickfix.QuickFixException;

public class ExpressionRenderer implements Renderer {
    @Override
    public void render(BaseComponent<?, ?> component, Appendable out) throws IOException, QuickFixException {

        RenderingService renderingService = Aura.getRenderingService();

        Object value = component.getAttributes().getValue("value");

        if (value instanceof Wrapper) {
            value = ((Wrapper) value).unwrap();
        }

        if (value instanceof String) {
            String escaped = (String) value;
            // This amounts to a convoluted test for "is the direct container
            // of this expression a template?"  We need that test because we use
            // "bad" characters like script tags in our own expressions, but want to
            // deny them from users.
            boolean inTemplate = component.getAttributes().getValueProvider()
                    .getDescriptor().getDef().isTemplate();

            // KRIS: 
            // Nasty hack. Done to allow script generation in the componentClass component.
            // We don't a way to prevent escaping explicitly on server generated components. This 
            // seems fine until we use one to do exactly what we are doing here. Soo... besides setting
            // a global flag to toggle this, I'm doing this ugly little hack. 
            boolean isClientComponentClass = component.getAttributes().getValueProvider()
                    .getDescriptor().getQualifiedName().equals("markup://auradev:componentClass");

            if (!inTemplate && !isClientComponentClass) {
                // We don't escape all the HTML characters, because quotes in particular
                // would cause problems.
                escaped = escaped.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            }
            out.append(escaped);
        } else if (value instanceof List) {
            List<?> kids = (List<?>) value;
            for (Object kid : kids) {
                if (kid instanceof BaseComponent) {
                    renderingService.render((BaseComponent<?, ?>) kid, out);
                } else if (kid instanceof ComponentDefRef) {
                    Component cmp = ((ComponentDefRef) kid).newInstance(component);
                    renderingService.render(cmp, out);
                }
            }
        } else if (value != null) {
            out.append(value.toString());
        }
    }
}
