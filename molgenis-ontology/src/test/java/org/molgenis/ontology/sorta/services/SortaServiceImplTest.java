package org.molgenis.ontology.sorta.services;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.QueryRule.Operator.AND;
import static org.molgenis.data.QueryRule.Operator.DIS_MAX;
import static org.molgenis.data.QueryRule.Operator.EQUALS;
import static org.molgenis.data.QueryRule.Operator.FUZZY_MATCH;
import static org.molgenis.data.QueryRule.Operator.FUZZY_MATCH_NGRAM;
import static org.molgenis.data.QueryRule.Operator.IN;
import static org.molgenis.data.meta.AttributeType.STRING;
import static org.molgenis.ontology.core.meta.OntologyMetadata.ONTOLOGY;
import static org.molgenis.ontology.core.meta.OntologyTermDynamicAnnotationMetadata.ONTOLOGY_TERM_DYNAMIC_ANNOTATION;
import static org.molgenis.ontology.core.meta.OntologyTermMetadata.ONTOLOGY_TERM;
import static org.molgenis.ontology.core.meta.OntologyTermMetadata.ONTOLOGY_TERM_NAME;
import static org.molgenis.ontology.sorta.meta.OntologyTermHitMetaData.COMBINED_SCORE;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.molgenis.data.AbstractMolgenisSpringTest;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.QueryRule;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.support.DynamicEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.config.OntologyTestConfig;
import org.molgenis.ontology.core.meta.Ontology;
import org.molgenis.ontology.core.meta.OntologyFactory;
import org.molgenis.ontology.core.meta.OntologyMetadata;
import org.molgenis.ontology.core.meta.OntologyTerm;
import org.molgenis.ontology.core.meta.OntologyTermDynamicAnnotation;
import org.molgenis.ontology.core.meta.OntologyTermDynamicAnnotationFactory;
import org.molgenis.ontology.core.meta.OntologyTermDynamicAnnotationMetadata;
import org.molgenis.ontology.core.meta.OntologyTermFactory;
import org.molgenis.ontology.core.meta.OntologyTermMetadata;
import org.molgenis.ontology.core.meta.OntologyTermSynonym;
import org.molgenis.ontology.core.meta.OntologyTermSynonymFactory;
import org.molgenis.ontology.roc.InformationContentService;
import org.molgenis.ontology.sorta.meta.OntologyTermHitMetaData;
import org.molgenis.ontology.sorta.service.impl.SortaServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@MockitoSettings(strictness = Strictness.LENIENT)
@ContextConfiguration(classes = {SortaServiceImplTest.Config.class})
class SortaServiceImplTest extends AbstractMolgenisSpringTest {
  private static final String ONTOLOGY_IRI = "http://www.molgenis.org/";

  @Autowired private SortaServiceImpl sortaServiceImpl;

  @Autowired private DataService dataService;

  @Autowired private OntologyFactory ontologyFactory;

  @Autowired private OntologyTermFactory ontologyTermFactory;

  @Autowired private OntologyTermSynonymFactory ontologyTermSynonymFactory;

  @Autowired private OntologyTermDynamicAnnotationFactory ontologyTermDynamicAnnotationFactory;

  @BeforeEach
  void beforeMethod() {
    // Mock ontology entity
    Ontology ontology = ontologyFactory.create();
    ontology.setOntologyIri(ONTOLOGY_IRI);

    // define dataService actions for test one
    when(dataService.findOne(
            ONTOLOGY, new QueryImpl<>().eq(OntologyMetadata.ONTOLOGY_IRI, ONTOLOGY_IRI)))
        .thenReturn(ontology);

    when(dataService.count(
            ONTOLOGY_TERM, new QueryImpl<>().eq(OntologyTermMetadata.ONTOLOGY, ontology)))
        .thenReturn((long) 100);

    QueryRule queryRule =
        new QueryRule(
            singletonList(
                new QueryRule(OntologyTermMetadata.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH, "hear")));
    queryRule.setOperator(DIS_MAX);
    when(dataService.count(ONTOLOGY_TERM, new QueryImpl<>(queryRule))).thenReturn((long) 50);

    QueryRule queryRule2 =
        new QueryRule(
            singletonList(
                new QueryRule(OntologyTermMetadata.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH, "impair")));
    queryRule2.setOperator(DIS_MAX);
    when(dataService.count(ONTOLOGY_TERM, new QueryImpl<>(queryRule2))).thenReturn((long) 50);

    when(dataService.findAll(ONTOLOGY))
        .thenReturn(Collections.<Entity>singletonList(ontology).stream());

    // ########################### TEST ONE ###########################
    // Mock the first ontology term entity only with name
    OntologyTermSynonym ontologyTermSynonym0 = ontologyTermSynonymFactory.create();
    ontologyTermSynonym0.setOntologyTermSynonym("hearing impairment");

    OntologyTerm ontologyTerm0 = ontologyTermFactory.create();
    ontologyTerm0.setId("1");
    ontologyTerm0.setOntology(ontology);
    ontologyTerm0.setOntologyTermName("hearing impairment");
    ontologyTerm0.setOntologyTermIri(ONTOLOGY_IRI + '1');
    ontologyTerm0.setOntologyTermSynonyms(singletonList(ontologyTermSynonym0));
    ontologyTerm0.setOntologyTermDynamicAnnotations(emptyList());

    // Mock the second ontology term entity only with name
    OntologyTermSynonym ontologyTermSynonym1 = ontologyTermSynonymFactory.create();
    ontologyTermSynonym1.setOntologyTermSynonym("mixed hearing impairment");

    OntologyTerm ontologyTerm1 = ontologyTermFactory.create();
    ontologyTerm1.setId("2");
    ontologyTerm1.setOntology(ontology);
    ontologyTerm1.setOntologyTermName("mixed hearing impairment");
    ontologyTerm1.setOntologyTermIri(ONTOLOGY_IRI + '2');
    ontologyTerm1.setOntologyTermSynonyms(singletonList(ontologyTermSynonym1));
    ontologyTerm1.setOntologyTermDynamicAnnotations(emptyList());

    // DataService action for regular matching ontology term synonyms
    QueryRule disMaxRegularQueryRule =
        new QueryRule(
            singletonList(
                new QueryRule(
                    OntologyTermMetadata.ONTOLOGY_TERM_SYNONYM,
                    FUZZY_MATCH,
                    "hear~0.8 impair~0.8")));
    disMaxRegularQueryRule.setOperator(DIS_MAX);

    List<QueryRule> finalQueryRules =
        asList(
            new QueryRule(OntologyTermMetadata.ONTOLOGY, EQUALS, ontology),
            new QueryRule(AND),
            disMaxRegularQueryRule);

    when(dataService.findAll(ONTOLOGY_TERM, new QueryImpl<>(finalQueryRules).pageSize(50)))
        .thenReturn(Arrays.<Entity>asList(ontologyTerm0, ontologyTerm1).stream());

    // DataService action for n-gram matching ontology term synonyms
    QueryRule disMaxNGramQueryRule =
        new QueryRule(
            singletonList(
                new QueryRule(
                    OntologyTermMetadata.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH_NGRAM, "hear impair")));
    disMaxNGramQueryRule.setOperator(DIS_MAX);
    when(dataService.findAll(
            ONTOLOGY_TERM,
            new QueryImpl<>(
                    asList(
                        new QueryRule(OntologyTermMetadata.ONTOLOGY, EQUALS, ontology),
                        new QueryRule(AND),
                        disMaxNGramQueryRule))
                .pageSize(10)))
        .thenReturn(Arrays.<Entity>asList(ontologyTerm0, ontologyTerm1).stream());

    // DataService action for querying specific ontology term based on ontologyIRI and
    // ontologyTermIRI
    when(dataService.findOne(
            ONTOLOGY_TERM,
            new QueryImpl<>()
                .eq(OntologyTermMetadata.ONTOLOGY_TERM_IRI, ONTOLOGY_IRI + '1')
                .and()
                .eq(OntologyTermMetadata.ONTOLOGY, ontology)))
        .thenReturn(ontologyTerm0);

    when(dataService.findOne(
            ONTOLOGY_TERM,
            new QueryImpl<>()
                .eq(OntologyTermMetadata.ONTOLOGY_TERM_IRI, ONTOLOGY_IRI + '2')
                .and()
                .eq(OntologyTermMetadata.ONTOLOGY, ontology)))
        .thenReturn(ontologyTerm1);

    // ########################### TEST TWO ###########################

    OntologyTermSynonym ontologyTermSynonym2 = ontologyTermSynonymFactory.create();
    ontologyTermSynonym2.setOntologyTermSynonym("ot_3");

    // Mock ontologyTermDynamicAnnotation entities
    OntologyTermDynamicAnnotation ontologyTermDynamicAnnotation_3_1 =
        ontologyTermDynamicAnnotationFactory.create();
    ontologyTermDynamicAnnotation_3_1.setName("OMIM");
    ontologyTermDynamicAnnotation_3_1.setValue("123456");
    ontologyTermDynamicAnnotation_3_1.setLabel("OMIM:123456");

    // Mock ontologyTerm entity based on the previous entities defined
    OntologyTerm ontologyTermEntity_3 = ontologyTermFactory.create();
    ontologyTermEntity_3.setId("3");
    ontologyTermEntity_3.setOntology(ontology);
    ontologyTermEntity_3.setOntologyTermName("ot_3");
    ontologyTermEntity_3.setOntologyTermIri(ONTOLOGY_IRI + '3');
    ontologyTermEntity_3.setOntologyTermSynonyms(
        singletonList(
            ontologyTermSynonym2)); // self reference intended? Arrays.asList(ontologyTermEntity_3)
    ontologyTermEntity_3.set(
        OntologyTermMetadata.ONTOLOGY_TERM_DYNAMIC_ANNOTATION,
        singletonList(ontologyTermDynamicAnnotation_3_1));

    // DataService action for matching ontology term annotation
    QueryRule annotationQueryRule =
        new QueryRule(
            asList(
                new QueryRule(OntologyTermDynamicAnnotationMetadata.NAME, EQUALS, "OMIM"),
                new QueryRule(AND),
                new QueryRule(OntologyTermDynamicAnnotationMetadata.VALUE, EQUALS, "123456")));

    when(dataService.findAll(
            ONTOLOGY_TERM_DYNAMIC_ANNOTATION,
            new QueryImpl<>(singletonList(annotationQueryRule)).pageSize(Integer.MAX_VALUE)))
        .thenReturn(Collections.<Entity>singletonList(ontologyTermDynamicAnnotation_3_1).stream());

    when(dataService.findAll(
            ONTOLOGY_TERM,
            new QueryImpl<>(
                    asList(
                        new QueryRule(OntologyTermMetadata.ONTOLOGY, EQUALS, ontology),
                        new QueryRule(AND),
                        new QueryRule(
                            OntologyTermMetadata.ONTOLOGY_TERM_DYNAMIC_ANNOTATION,
                            IN,
                            singletonList(ontologyTermDynamicAnnotation_3_1))))
                .pageSize(Integer.MAX_VALUE)))
        .thenReturn(Collections.<Entity>singletonList(ontologyTermEntity_3).stream());

    // DataService action for elasticsearch regular matching ontology term synonyms
    QueryRule disMaxRegularQueryRule_2 =
        new QueryRule(
            singletonList(
                new QueryRule(
                    OntologyTermMetadata.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH, "input~0.8")));
    disMaxRegularQueryRule_2.setOperator(DIS_MAX);
    when(dataService.findAll(
            ONTOLOGY_TERM,
            new QueryImpl<>(
                    asList(
                        new QueryRule(OntologyTermMetadata.ONTOLOGY, EQUALS, ontology),
                        new QueryRule(AND),
                        disMaxRegularQueryRule_2))
                .pageSize(49)))
        .thenReturn(Stream.empty());

    // DataService action for n-gram matching ontology term synonyms
    QueryRule disMaxNGramQueryRule_2 =
        new QueryRule(
            singletonList(
                new QueryRule(
                    OntologyTermMetadata.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH_NGRAM, "input")));
    disMaxNGramQueryRule_2.setOperator(DIS_MAX);
    when(dataService.findAll(
            ONTOLOGY_TERM,
            new QueryImpl<>(
                    asList(
                        new QueryRule(OntologyTermMetadata.ONTOLOGY, EQUALS, ontology),
                        new QueryRule(AND),
                        disMaxNGramQueryRule_2))
                .pageSize(10)))
        .thenReturn(Stream.empty());

    // ########################### TEST THREE ###########################
    // Define the input for test three

    OntologyTermSynonym ontologyTermSynonym_4_1 = ontologyTermSynonymFactory.create();
    ontologyTermSynonym_4_1.setOntologyTermSynonym("protruding eye");

    OntologyTermSynonym ontologyTermSynonym_4_2 = ontologyTermSynonymFactory.create();
    ontologyTermSynonym_4_2.setOntologyTermSynonym("proptosis");

    OntologyTermSynonym ontologyTermSynonym_4_3 = ontologyTermSynonymFactory.create();
    ontologyTermSynonym_4_3.setOntologyTermSynonym("Exophthalmos");

    // Mock ontologyTerm entity based on the previous entities defined
    OntologyTerm ontologyTermEntity_4 = ontologyTermFactory.create();
    ontologyTermEntity_4.setId("4");
    ontologyTermEntity_4.setOntology(ontology);
    ontologyTermEntity_4.setOntologyTermName("protruding eye");
    ontologyTermEntity_4.setOntologyTermIri(ONTOLOGY_IRI + '4');
    ontologyTermEntity_4.setOntologyTermSynonyms(
        asList(ontologyTermSynonym_4_1, ontologyTermSynonym_4_2, ontologyTermSynonym_4_3));
    ontologyTermEntity_4.setOntologyTermDynamicAnnotations(emptyList());

    // DataService action for elasticsearch regular matching ontology term synonyms
    QueryRule disMaxRegularQueryRule_3 =
        new QueryRule(
            singletonList(
                new QueryRule(
                    OntologyTermMetadata.ONTOLOGY_TERM_SYNONYM,
                    FUZZY_MATCH,
                    "proptosi~0.8 protrud~0.8 ey~0.8 exophthalmo~0.8")));
    disMaxRegularQueryRule_3.setOperator(DIS_MAX);

    when(dataService.findAll(
            ONTOLOGY_TERM,
            new QueryImpl<>(
                    asList(
                        new QueryRule(OntologyTermMetadata.ONTOLOGY, EQUALS, ontology),
                        new QueryRule(AND),
                        disMaxRegularQueryRule_3))
                .pageSize(50)))
        .thenReturn(Collections.<Entity>singletonList(ontologyTermEntity_4).stream());

    // DataService action for elasticsearch ngram matching ontology term synonyms
    QueryRule disMaxNGramQueryRule_3 =
        new QueryRule(
            singletonList(
                new QueryRule(
                    OntologyTermMetadata.ONTOLOGY_TERM_SYNONYM,
                    FUZZY_MATCH_NGRAM,
                    "proptosi protrud ey exophthalmo")));
    disMaxNGramQueryRule_3.setOperator(QueryRule.Operator.DIS_MAX);

    when(dataService.findAll(
            ONTOLOGY_TERM,
            new QueryImpl<>(
                    asList(
                        new QueryRule(OntologyTermMetadata.ONTOLOGY, EQUALS, ontology),
                        new QueryRule(AND),
                        disMaxNGramQueryRule_3))
                .pageSize(10)))
        .thenReturn(Collections.<Entity>singletonList(ontologyTermEntity_4).stream());
  }

  @Test
  void findOntologyTermEntities() {
    Attribute nameAttr = when(mock(Attribute.class).getName()).thenReturn("Name").getMock();
    when(nameAttr.getDataType()).thenReturn(STRING);
    Attribute omimAttr = when(mock(Attribute.class).getName()).thenReturn("OMIM").getMock();
    when(omimAttr.getDataType()).thenReturn(STRING);

    EntityType entityType = mock(EntityType.class);
    when(entityType.getAtomicAttributes()).thenReturn(asList(nameAttr, omimAttr));
    when(entityType.getAttribute("Name")).thenReturn(nameAttr);
    when(entityType.getAttribute("OMIM")).thenReturn(omimAttr);

    // Test one: match only the name of input with ontology terms
    Entity firstInput = new DynamicEntity(entityType);
    firstInput.set("Name", "hearing impairment");

    Iterable<Entity> ontologyTerms_test1 =
        sortaServiceImpl.findOntologyTermEntities(ONTOLOGY_IRI, firstInput);
    Iterator<Entity> iterator_test1 = ontologyTerms_test1.iterator();

    assertEquals(true, iterator_test1.hasNext());
    Entity firstMatch_test1 = iterator_test1.next();
    assertEquals(100, firstMatch_test1.getDouble(COMBINED_SCORE).intValue());

    assertEquals(true, iterator_test1.hasNext());
    Entity secondMatch_test1 = iterator_test1.next();
    assertEquals(new Double(85).intValue(), secondMatch_test1.getDouble(COMBINED_SCORE).intValue());

    assertEquals(false, iterator_test1.hasNext());

    // Test two: match the database annotation of input with ontology terms
    Entity secondInput = new DynamicEntity(entityType);
    secondInput.set("Name", "input");
    secondInput.set("OMIM", "123456");

    Iterable<Entity> ontologyTerms_test2 =
        sortaServiceImpl.findOntologyTermEntities(ONTOLOGY_IRI, secondInput);
    Iterator<Entity> iterator_test2 = ontologyTerms_test2.iterator();

    assertEquals(true, iterator_test2.hasNext());
    Entity firstMatch_test2 = iterator_test2.next();
    assertEquals(100, firstMatch_test2.getDouble(COMBINED_SCORE).intValue());

    assertEquals(false, iterator_test2.hasNext());

    // Test three: match only the name of input with ontology terms, since the name contains
    // multiple synonyms
    // therefore add up all the scores from synonyms
    Entity thirdInput = new DynamicEntity(entityType);
    thirdInput.set("Name", "proptosis, protruding eye, Exophthalmos ");

    Iterable<Entity> ontologyTerms_test3 =
        sortaServiceImpl.findOntologyTermEntities(ONTOLOGY_IRI, thirdInput);
    Iterator<Entity> iterator_test3 = ontologyTerms_test3.iterator();

    assertEquals(true, iterator_test3.hasNext());
    Entity firstMatch_test3 = iterator_test3.next();
    assertEquals(100, firstMatch_test3.getDouble(COMBINED_SCORE).intValue());

    assertEquals(false, iterator_test3.hasNext());
  }

  @Test
  void getAllOntologyEntities() {
    Iterable<Entity> allOntologyEntities = sortaServiceImpl.getAllOntologyEntities();

    Iterator<Entity> iterator = allOntologyEntities.iterator();

    assertEquals(true, iterator.hasNext());

    Entity ontologyEntity = iterator.next();

    assertEquals(ONTOLOGY_IRI, ontologyEntity.getString(OntologyMetadata.ONTOLOGY_IRI));

    assertEquals(false, iterator.hasNext());
  }

  @Test
  void getOntologyEntity() {
    Entity ontologyEntity = sortaServiceImpl.getOntologyEntity(ONTOLOGY_IRI);
    assertEquals(ontologyEntity.getString(OntologyMetadata.ONTOLOGY_IRI), ONTOLOGY_IRI);
  }

  @Test
  void getOntologyTermEntity() {
    Entity firstOntologyTermEntity =
        sortaServiceImpl.getOntologyTermEntity(ONTOLOGY_IRI + 1, ONTOLOGY_IRI);
    assertEquals("hearing impairment", firstOntologyTermEntity.getString(ONTOLOGY_TERM_NAME));

    Entity secondOntologyTermEntity =
        sortaServiceImpl.getOntologyTermEntity(ONTOLOGY_IRI + 2, ONTOLOGY_IRI);
    assertEquals(
        "mixed hearing impairment", secondOntologyTermEntity.getString(ONTOLOGY_TERM_NAME));
  }

  @Configuration
  @Import({OntologyTestConfig.class, OntologyTermHitMetaData.class})
  static class Config {
    @Autowired private DataService dataService;

    @Autowired OntologyTermHitMetaData ontologyTermHitMetaData;

    @Autowired private OntologyTermSynonymFactory ontologyTermSynonymFactory;

    @Bean
    InformationContentService informationContentService() {
      return mock(InformationContentService.class);
    }

    @Bean
    SortaServiceImpl sortaServiceImpl() {
      return new SortaServiceImpl(
          dataService,
          informationContentService(),
          ontologyTermHitMetaData,
          ontologyTermSynonymFactory);
    }
  }
}
