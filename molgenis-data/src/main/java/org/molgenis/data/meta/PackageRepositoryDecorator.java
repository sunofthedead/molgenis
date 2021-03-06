package org.molgenis.data.meta;

import static com.google.common.collect.Streams.stream;
import static java.util.Objects.requireNonNull;
import static org.molgenis.data.meta.model.EntityTypeMetadata.ENTITY_TYPE_META_DATA;

import java.util.stream.Stream;
import org.molgenis.data.AbstractRepositoryDecorator;
import org.molgenis.data.DataService;
import org.molgenis.data.Repository;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.meta.model.Package;
import org.molgenis.data.util.PackageUtils.PackageTreeTraverser;

public class PackageRepositoryDecorator extends AbstractRepositoryDecorator<Package> {
  private final DataService dataService;

  public PackageRepositoryDecorator(
      Repository<Package> delegateRepository, DataService dataService) {
    super(delegateRepository);
    this.dataService = requireNonNull(dataService);
  }

  @Override
  public void delete(Package entity) {
    deletePackage(entity);
  }

  @Override
  public void delete(Stream<Package> entities) {
    entities.forEach(this::deletePackage);
  }

  @Override
  public void deleteById(Object id) {
    deletePackage(findOneById(id));
  }

  @Override
  public void deleteAll(Stream<Object> ids) {
    findAll(ids).forEach(this::deletePackage);
  }

  @Override
  public void deleteAll() {
    forEach(this::deletePackage);
  }

  private void deletePackage(Package aPackage) {
    deleteEntityTypesInPackageAndSubPackages(aPackage);

    // delete rows from package table
    delegate().delete(getPackageTreeTraversal(aPackage));
  }

  private void deleteEntityTypesInPackageAndSubPackages(Package aPackage) {
    Repository<EntityType> entityRepo = getEntityRepository();
    Stream<EntityType> entityTypesToDelete =
        getPackageTreeTraversal(aPackage).flatMap(p -> stream(p.getEntityTypes()));
    entityRepo.delete(entityTypesToDelete);
  }

  private static Stream<Package> getPackageTreeTraversal(Package aPackage) {
    return stream(new PackageTreeTraverser().postOrderTraversal(aPackage));
  }

  private Repository<EntityType> getEntityRepository() {
    return dataService.getRepository(ENTITY_TYPE_META_DATA, EntityType.class);
  }
}
