package org.molgenis.data.cache.l3;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.molgenis.data.EntityManager.CreationMode.NO_POPULATE;
import static org.molgenis.data.RepositoryCapability.CACHEABLE;
import static org.molgenis.data.meta.AttributeType.INT;
import static org.molgenis.data.meta.model.EntityType.AttributeRole.ROLE_ID;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.molgenis.data.AbstractMolgenisSpringTest;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityManager;
import org.molgenis.data.Fetch;
import org.molgenis.data.Query;
import org.molgenis.data.Repository;
import org.molgenis.data.Sort;
import org.molgenis.data.meta.model.AttributeFactory;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.meta.model.EntityTypeFactory;
import org.molgenis.data.support.DynamicEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.data.transaction.TransactionInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@MockitoSettings(strictness = Strictness.LENIENT)
@ContextConfiguration(classes = L3CacheRepositoryDecoratorTest.Config.class)
class L3CacheRepositoryDecoratorTest extends AbstractMolgenisSpringTest {
  private L3CacheRepositoryDecorator l3CacheRepositoryDecorator;
  private EntityType entityType;

  private Entity entity1;
  private Entity entity2;
  private Entity entity3;

  private final String repositoryName = "TestRepository";
  private static final String COUNTRY = "Country";
  private static final String ID = "ID";

  @Mock private L3Cache l3Cache;

  @Mock private Repository<Entity> delegateRepository;

  @Mock private TransactionInformation transactionInformation;

  @Autowired private EntityTypeFactory entityTypeFactory;

  @Autowired private AttributeFactory attributeFactory;

  @Autowired private EntityManager entityManager;

  @Captor private ArgumentCaptor<Stream<Object>> entityIdCaptor;

  private Query<Entity> query;

  @Mock private Fetch fetch;

  @BeforeEach
  void beforeMethod() {
    entityType = entityTypeFactory.create(repositoryName);
    entityType.addAttribute(attributeFactory.create().setDataType(INT).setName(ID), ROLE_ID);
    entityType.addAttribute(attributeFactory.create().setName(COUNTRY));

    when(entityManager.create(entityType, NO_POPULATE)).thenReturn(new DynamicEntity(entityType));

    entity1 = entityManager.create(entityType, NO_POPULATE);
    entity1.set(ID, 1);
    entity1.set(COUNTRY, "NL");

    entity2 = entityManager.create(entityType, NO_POPULATE);
    entity2.set(ID, 2);
    entity2.set(COUNTRY, "NL");

    entity3 = entityManager.create(entityType, NO_POPULATE);
    entity3.set(ID, 3);
    entity3.set(COUNTRY, "GB");

    when(delegateRepository.getCapabilities()).thenReturn(Sets.newHashSet(CACHEABLE));
    l3CacheRepositoryDecorator =
        new L3CacheRepositoryDecorator(delegateRepository, l3Cache, transactionInformation);
    verify(delegateRepository).getCapabilities();
    query = new QueryImpl<>().eq(COUNTRY, "GB");
    query.pageSize(10);
    query.sort(new Sort().on(COUNTRY));
    query.setFetch(fetch);
    when(delegateRepository.getEntityType()).thenReturn(entityType);
    when(delegateRepository.getName()).thenReturn(repositoryName);
  }

  @Test
  void testFindOneRepositoryClean() {
    when(transactionInformation.isRepositoryCompletelyClean(entityType)).thenReturn(true);
    Query<Entity> queryWithPageSizeOne = new QueryImpl<>(query).pageSize(1);
    when(l3Cache.get(delegateRepository, queryWithPageSizeOne)).thenReturn(singletonList(3));
    when(delegateRepository.findOneById(3, fetch)).thenReturn(entity3);

    assertEquals(entity3, l3CacheRepositoryDecorator.findOne(queryWithPageSizeOne));
    verify(delegateRepository, times(1)).findOneById(3, fetch);
    verify(delegateRepository, atLeast(0)).getEntityType();
    verifyNoMoreInteractions(delegateRepository);
  }

  @Test
  void testFindOneRepositoryDirty() {
    when(transactionInformation.isRepositoryCompletelyClean(entityType)).thenReturn(false);
    when(delegateRepository.findOne(query)).thenReturn(entity3);

    assertEquals(entity3, l3CacheRepositoryDecorator.findOne(query));
    verifyNoMoreInteractions(l3Cache);
  }

  @Test
  void testFindAllRepositoryClean() {
    when(transactionInformation.isRepositoryCompletelyClean(entityType)).thenReturn(true);

    List<Object> ids = asList(1, 2);
    List<Entity> expectedEntities = newArrayList(entity1, entity2);

    when(l3Cache.get(delegateRepository, query)).thenReturn(ids);
    when(delegateRepository.findAll(entityIdCaptor.capture(), eq(query.getFetch())))
        .thenReturn(expectedEntities.stream());

    Stream<Entity> actualEntities = l3CacheRepositoryDecorator.findAll(query);

    assertEquals(expectedEntities, actualEntities.collect(toList()));
    assertEquals(ids, entityIdCaptor.getValue().collect(toList()));
  }

  @Test
  void testFindAllVeryLargePageSize() {
    when(transactionInformation.isRepositoryCompletelyClean(entityType)).thenReturn(true);
    Query<Entity> largeQuery = new QueryImpl<>(query).setPageSize(10000);

    List<Entity> expectedEntities = newArrayList(entity1, entity2);

    when(delegateRepository.findAll(largeQuery)).thenReturn(expectedEntities.stream());

    List<Entity> actualEntities = l3CacheRepositoryDecorator.findAll(largeQuery).collect(toList());

    assertEquals(expectedEntities, actualEntities);
    verifyNoMoreInteractions(l3Cache);
  }

  @Test
  void testFindAllZeroPageSize() {
    when(transactionInformation.isRepositoryCompletelyClean(entityType)).thenReturn(true);
    Query<Entity> largeQuery = new QueryImpl<>(query).setPageSize(0);

    List<Entity> expectedEntities = newArrayList(entity1, entity2);

    when(delegateRepository.findAll(largeQuery)).thenReturn(expectedEntities.stream());

    List<Entity> actualEntities = l3CacheRepositoryDecorator.findAll(largeQuery).collect(toList());

    assertEquals(expectedEntities, actualEntities);
    verifyNoMoreInteractions(l3Cache);
  }

  @Test
  void testFindAllRepositoryDirty() {
    when(transactionInformation.isRepositoryCompletelyClean(entityType)).thenReturn(false);
    Query<Entity> query = new QueryImpl<>().eq(COUNTRY, "NL");
    query.pageSize(10);
    query.sort(new Sort());
    query.fetch(new Fetch());

    List<Entity> expectedEntities = newArrayList(entity1, entity2);

    when(delegateRepository.findAll(query)).thenReturn(expectedEntities.stream());

    List<Entity> actualEntities = l3CacheRepositoryDecorator.findAll(query).collect(toList());

    assertEquals(expectedEntities, actualEntities);
    verifyNoMoreInteractions(l3Cache);
  }

  @Configuration
  static class Config {
    @Bean
    EntityManager entityManager() {
      return mock(EntityManager.class);
    }
  }
}
