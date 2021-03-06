package org.molgenis.data.vcf.utils.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.molgenis.data.AbstractMolgenisSpringTest;
import org.molgenis.data.Entity;
import org.molgenis.data.vcf.model.VcfAttributes;
import org.molgenis.data.vcf.utils.VcfUtils;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {VcfUtilsTest.Config.class})
public class VcfUtilsTest extends AbstractMolgenisSpringTest {

  @Test
  public void createId() {
    Entity entity = mock(Entity.class);
    doReturn("1").when(entity).get(VcfAttributes.CHROM);
    doReturn(10050000).when(entity).get(VcfAttributes.POS);
    doReturn("test21").when(entity).get(VcfAttributes.ID);
    doReturn("G").when(entity).get(VcfAttributes.REF);
    doReturn("A").when(entity).get(VcfAttributes.ALT);
    doReturn(".").when(entity).get(VcfAttributes.QUAL);
    doReturn("PASS").when(entity).get(VcfAttributes.FILTER);
    assertEquals("VWnsXKOj5B7PBk4dwFLsQw", VcfUtils.createId(entity));
  }
}
