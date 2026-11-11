package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;

import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.standalone.context.SimpleWorkerContext;
import org.hl7.fhir.services.utilities.NPMPackageGenerator;
import org.hl7.fhir.utilities.json.model.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

/** In watch mode the builds of a session make the same package of the same content */
class PackageTimestampTest {

  @TempDir
  Path dir;

  @Test
  void theValidationSummaryHasTheSessionsTime() throws Exception {
    Calendar session = Calendar.getInstance();
    session.add(Calendar.HOUR, -1);
    byte[] first = buildPackage(session, dir.resolve("first.tgz"));
    Thread.sleep(5);
    byte[] second = buildPackage(session, dir.resolve("second.tgz"));
    assertArrayEquals(first, second);
  }

  private byte[] buildPackage(Calendar session, Path tgz) throws IOException {
    PublisherFields pf = new PublisherFields();
    pf.pinExecTime(session);
    pf.context = Mockito.mock(SimpleWorkerContext.class);
    Mockito.when(pf.context.getModelContext()).thenReturn(ModelContext.fullCoreContext());
    JsonObject p = new JsonObject();
    p.add("name", "example.ig");
    p.add("version", "0.1.0");
    pf.npm = new NPMPackageGenerator(tgz.toString(), p, pf.getExecTime().getTime(), true);
    PublisherGenerator gen = new PublisherGenerator(new PublisherSettings());
    gen.setPf(pf);
    gen.addFileToNpm(NPMPackageGenerator.Category.OTHER, "validation-oo.json", gen.validationSummaryOO());
    pf.npm.finish();
    return Files.readAllBytes(tgz);
  }
}
