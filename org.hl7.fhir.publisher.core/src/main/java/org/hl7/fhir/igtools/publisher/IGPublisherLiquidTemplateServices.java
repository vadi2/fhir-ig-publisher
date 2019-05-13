package org.hl7.fhir.igtools.publisher;

/*-
 * #%L
 * org.hl7.fhir.publisher.core
 * %%
 * Copyright (C) 2014 - 2019 Health Level 7
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.utils.NarrativeGenerator.ILiquidTemplateProvider;
import org.hl7.fhir.r5.utils.NarrativeGenerator.ResourceContext;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;

public class IGPublisherLiquidTemplateServices implements ILiquidTemplateProvider {

  Map<String, String> templates = new HashMap<>();
  
  public void clear() {
    templates.clear();
  }
  
  public void load(String path) throws FileNotFoundException, IOException {
    for (File f : new File(path).listFiles()) {
      if (f.getName().endsWith(".liquid")) {
        String fn = f.getName();
        fn = fn.substring(0, fn.indexOf("."));
        templates.put(fn.toLowerCase(), TextFile.fileToString(f));
      }
    }
  }

  @Override
  public String findTemplate(ResourceContext rcontext, DomainResource r) {
    String s = r.fhirType();
    return templates.get(s.toLowerCase());
  }

}