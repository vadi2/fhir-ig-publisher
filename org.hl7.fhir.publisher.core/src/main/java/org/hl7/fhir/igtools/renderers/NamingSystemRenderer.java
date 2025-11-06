package org.hl7.fhir.igtools.renderers;

/*-
 * #%L
 * org.hl7.fhir.publisher.core
 * %%
 * Copyright (C) 2014 - 2025 Health Level 7
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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.igtools.publisher.IGKnowledgeProvider;
import org.hl7.fhir.igtools.publisher.RelatedIG;
import org.hl7.fhir.igtools.publisher.SpecMapManager;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.model.CanonicalResource;
import org.hl7.fhir.r5.model.NamingSystem;
import org.hl7.fhir.r5.model.NamingSystem.NamingSystemUniqueIdComponent;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r5.renderers.utils.RenderingContext;
import org.hl7.fhir.utilities.MarkDownProcessor;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.npm.NpmPackage;

public class NamingSystemRenderer extends CanonicalRenderer {

  private NamingSystem ns;

  public NamingSystemRenderer(IWorkerContext context, String corePath, NamingSystem ns, IGKnowledgeProvider igp, List<SpecMapManager> maps, Set<String> allTargets, MarkDownProcessor markdownEngine, NpmPackage packge, RenderingContext gen, String versionToAnnotate, List<RelatedIG> relatedIgs) {
    super(context, corePath, ns, null, igp, maps, allTargets, markdownEngine, packge, gen, versionToAnnotate, relatedIgs);
    this.ns = ns;
  }

  @Override
  protected void genSummaryRowsSpecific(StringBuilder b, Set<String> rows) {
    // NamingSystem-specific summary rows can be added here in the future
  }

  /**
   * Generate cross-reference HTML showing which ValueSets use the code system identifiers
   * described in this NamingSystem.
   *
   * This method extracts all URI-type uniqueId values from the NamingSystem and searches
   * all local ValueSet resources to find which ones reference those URIs in their compose
   * sections (includes or excludes).
   *
   * @return HTML string containing an unordered list of ValueSets that use this NamingSystem's identifiers
   * @throws FHIRException if there's an error processing the resources
   */
  public String xref() throws FHIRException {
    StringBuilder b = new StringBuilder();
    boolean first = true;
    b.append("\r\n");

    // Extract all URI-type uniqueId values from the NamingSystem
    List<String> nsUris = new ArrayList<>();
    for (NamingSystemUniqueIdComponent uid : ns.getUniqueId()) {
      if (uid.hasValue()) {
        String value = uid.getValue();
        // Add the URI if it's not already in the list
        if (!nsUris.contains(value)) {
          nsUris.add(value);
        }
      }
    }

    // If there are no URIs in the NamingSystem, return early
    if (nsUris.isEmpty()) {
      b.append("<ul><li>This NamingSystem has no identifiers to reference in value sets</li></ul>\r\n");
      return b.toString() + changeSummary();
    }

    // Scan all local ValueSet resources to find references
    Set<String> processed = new HashSet<>();
    for (CanonicalResource cr : scanAllLocalResources(ValueSet.class, "ValueSet")) {
      ValueSet vs = (ValueSet) cr;

      // Check includes
      for (ConceptSetComponent include : vs.getCompose().getInclude()) {
        first = addLinkForNS(b, first, vs, include, nsUris, processed);
      }

      // Check excludes
      for (ConceptSetComponent exclude : vs.getCompose().getExclude()) {
        first = addLinkForNS(b, first, vs, exclude, nsUris, processed);
      }
    }

    // Handle case where no ValueSets reference this NamingSystem
    if (first) {
      b.append("<ul><li>The Code System identifiers in this NamingSystem are not used in any value sets in this Implementation Guide</li></ul>\r\n");
    } else {
      b.append("</ul>\r\n");
    }

    return b.toString() + changeSummary();
  }

  /**
   * Helper method to add a link to a ValueSet if it references any of the NamingSystem URIs
   *
   * @param b StringBuilder to append the HTML to
   * @param first Whether this is the first link being added
   * @param vs The ValueSet to potentially link to
   * @param conceptSet The ConceptSetComponent (include or exclude) to check
   * @param nsUris List of URIs from the NamingSystem
   * @param processed Set of already processed ValueSet paths to avoid duplicates
   * @return Updated first flag (false if a link was added)
   */
  private boolean addLinkForNS(StringBuilder b, boolean first, ValueSet vs, ConceptSetComponent conceptSet, List<String> nsUris, Set<String> processed) {
    if (conceptSet.hasSystem()) {
      String system = conceptSet.getSystem();

      // Check if this system matches any of the NamingSystem URIs
      if (nsUris.contains(system)) {
        String path = vs.getWebPath();

        // Only add if we haven't already added this ValueSet
        if (!processed.contains(path)) {
          if (first) {
            first = false;
            b.append("<ul>\r\n");
          }

          if (path == null) {
            System.out.println("No path for " + vs.getUrl());
          } else {
            b.append(" <li><a href=\"" + path + "\">" + Utilities.escapeXml(gen.getTranslated(vs.getNameElement())) + "</a></li>\r\n");
          }

          processed.add(path);
        }
      }
    }
    return first;
  }
}
