package org.molgenis.data;

import org.molgenis.data.support.MapEntity;
import org.molgenis.security.core.Permission;
import org.molgenis.security.core.runas.SystemSecurityToken;
import org.molgenis.security.core.utils.SecurityUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.*;

import static java.util.Objects.requireNonNull;

public class RowLevelSecurityRepositoryDecorator implements Repository
{
	public static final String UPDATE_ATTRIBUTE = "_" + Permission.UPDATE.toString();
	public static final List<String> ROW_LEVEL_SECURITY_ATTRIBUTES = Arrays.asList(UPDATE_ATTRIBUTE);

	private final Repository decoratedRepository;
	private final RowLevelSecurityPermissionValidator permissionValidator;

	public RowLevelSecurityRepositoryDecorator(Repository decoratedRepository,
			RowLevelSecurityPermissionValidator rowLevelSecurityPermissionValidator)
	{
		this.decoratedRepository = requireNonNull(decoratedRepository);
		this.permissionValidator = requireNonNull(rowLevelSecurityPermissionValidator);
	}

	@Override
	public Stream<Entity> stream(Fetch fetch)
	{
		return decoratedRepository.stream(fetch).map(this::injectPermissions);
	}

	@Override
	public void close() throws IOException
	{
		decoratedRepository.close();
	}

	@Override
	public Set<RepositoryCapability> getCapabilities()
	{
		return decoratedRepository.getCapabilities();
	}

	@Override
	public String getName()
	{
		return decoratedRepository.getName();
	}

	@Override
	public EntityMetaData getEntityMetaData()
	{
		if (isRowLevelSecured())
		{
			return new RowLevelSecurityEntityMetaData(decoratedRepository.getEntityMetaData());
		}
		else
		{
			return decoratedRepository.getEntityMetaData();
		}
	}

	@Override
	public long count()
	{
		return decoratedRepository.count();
	}

	@Override
	public Iterator<Entity> iterator()
	{
		return decoratedRepository.iterator();
	}

	@Override
	public Query query()
	{
		return decoratedRepository.query();
	}

	@Override
	public long count(Query q)
	{
		return decoratedRepository.count(q);
	}

	@Override
	public Stream<Entity> findAll(Query q)
	{
		if (isRowLevelSecured())
		{
			return decoratedRepository.findAll(q).map(this::injectPermissions);
		}
		else
		{
			return decoratedRepository.findAll(q);
		}
	}

	@Override
	public Entity findOne(Query q)
	{
		if (isRowLevelSecured())
		{
			return injectPermissions(decoratedRepository.findOne(q));
		}
		else
		{
			return decoratedRepository.findOne(q);
		}
	}

	@Override
	public Entity findOne(Object id)
	{
		if (isRowLevelSecured())
		{
			return injectPermissions(decoratedRepository.findOne(id));
		}
		else
		{
			return decoratedRepository.findOne(id);
		}
	}

	@Override
	public Entity findOne(Object id, Fetch fetch)
	{
		if (isRowLevelSecured())
		{
			return injectPermissions(decoratedRepository.findOne(id, fetch));
		}
		else
		{
			return decoratedRepository.findOne(id, fetch);
		}
	}

	@Override
	public Stream<Entity> findAll(Stream<Object> ids)
	{
		if (isRowLevelSecured())
		{
			return decoratedRepository.findAll(ids).map(this::injectPermissions);
		}
		else
		{
			return decoratedRepository.findAll(ids);
		}
	}

	@Override
	public Stream<Entity> findAll(Stream<Object> ids, Fetch fetch)
	{
		if (isRowLevelSecured())
		{
			return decoratedRepository.findAll(ids, fetch).map(this::injectPermissions);
		}
		else
		{
			return decoratedRepository.findAll(ids, fetch);
		}
	}

	@Override
	public AggregateResult aggregate(AggregateQuery aggregateQuery)
	{
		return decoratedRepository.aggregate(aggregateQuery);
	}

	@Override
	public void update(Entity entity)
	{
		if (isRowLevelSecured())
		{
			if (!SecurityUtils.currentUserIsSu() && !SecurityUtils.currentUserHasRole(SystemSecurityToken.ROLE_SYSTEM))
			{
				permissionValidator.validatePermission(entity, Permission.UPDATE);
			}
		}
		decoratedRepository.update(entity);
	}

	@Override
	public void update(Stream<? extends Entity> entities)
	{
		if (isRowLevelSecured())
		{
			if (!SecurityUtils.currentUserIsSu() && !SecurityUtils.currentUserHasRole(SystemSecurityToken.ROLE_SYSTEM))
			{
				entities = entities.filter(entity -> permissionValidator.validatePermission(entity, Permission.UPDATE));
			}
		}
		decoratedRepository.update(entities);
	}

	@Override
	public void delete(Entity entity)
	{
		decoratedRepository.delete(entity);
	}

	@Override
	public void delete(Stream<? extends Entity> entities)
	{
		decoratedRepository.delete(entities);
	}

	@Override
	public void deleteById(Object id)
	{
		decoratedRepository.deleteById(id);
	}

	@Override
	public void deleteById(Stream<Object> ids)
	{
		decoratedRepository.deleteById(ids);
	}

	@Override
	public void deleteAll()
	{
		decoratedRepository.deleteAll();
	}

	@Override
	public void add(Entity entity)
	{
		decoratedRepository.add(entity);
	}

	@Override
	public Integer add(Stream<? extends Entity> entities)
	{
		return decoratedRepository.add(entities);
	}

	@Override
	public void flush()
	{
		decoratedRepository.flush();
	}

	@Override
	public void clearCache()
	{
		decoratedRepository.clearCache();
	}

	@Override
	public void create()
	{
		decoratedRepository.create();
	}

	@Override
	public void drop()
	{
		decoratedRepository.drop();
	}

	@Override
	public void rebuildIndex()
	{
		decoratedRepository.rebuildIndex();
	}

	@Override
	public void addEntityListener(EntityListener entityListener)
	{
		decoratedRepository.addEntityListener(entityListener);
	}

	@Override
	public void removeEntityListener(EntityListener entityListener)
	{
		decoratedRepository.removeEntityListener(entityListener);
	}

	private boolean isRowLevelSecured()
	{
		return decoratedRepository.getEntityMetaData().isRowLevelSecured();
	}

	private Entity injectPermissions(Entity entity)
	{
		boolean userHasPermissionToEditEntity = permissionValidator.userHasUpdatePermissionOnEntity(entity, Permission.UPDATE);
		Entity permissionEntity = new MapEntity(entity, new RowLevelSecurityEntityMetaData(entity.getEntityMetaData()));

		if(userHasPermissionToEditEntity)
		{
			// Current user is allowed to update
			permissionEntity.set("_PERMISSIONS", true);
			return permissionEntity;
		}
		else
		{
			// Current user does not have update permissions on this row
			permissionEntity.set("_PERMISSIONS", false);
			return permissionEntity;
		}
	}

	private class RowLevelSecurityEntityMetaData implements EntityMetaData
	{
		private EntityMetaData entityMetaData;

		public RowLevelSecurityEntityMetaData(EntityMetaData entityMetaData)
		{
			this.entityMetaData = entityMetaData;
		}

		@Override
		public Package getPackage()
		{
			return entityMetaData.getPackage();
		}

		@Override
		public String getName()
		{
			return entityMetaData.getName();
		}

		@Override
		public String getSimpleName()
		{
			return entityMetaData.getSimpleName();
		}

		@Override
		public String getBackend()
		{
			return entityMetaData.getBackend();
		}

		@Override
		public boolean isAbstract()
		{
			return entityMetaData.isAbstract();
		}

		@Override
		public String getLabel()
		{
			return entityMetaData.getLabel();
		}

		@Override
		public String getLabel(String languageCode)
		{
			return entityMetaData.getLabel(languageCode);
		}

		@Override
		public Set<String> getLabelLanguageCodes()
		{
			return entityMetaData.getLabelLanguageCodes();
		}

		@Override
		public String getDescription()
		{
			return entityMetaData.getDescription();
		}

		@Override
		public String getDescription(String languageCode)
		{
			return entityMetaData.getDescription(languageCode);
		}

		@Override
		public Set<String> getDescriptionLanguageCodes()
		{
			return entityMetaData.getDescriptionLanguageCodes();
		}

		@Override
		public Iterable<AttributeMetaData> getAttributes()
		{
			return filterPermissionAttributes(entityMetaData.getAttributes());
		}

		@Override
		public Iterable<AttributeMetaData> getOwnAttributes()
		{
			return filterPermissionAttributes(entityMetaData.getOwnAttributes());
		}

		@Override
		public Iterable<AttributeMetaData> getAtomicAttributes()
		{
			return filterPermissionAttributes(entityMetaData.getAtomicAttributes());
		}

		@Override
		public Iterable<AttributeMetaData> getOwnAtomicAttributes()
		{
			return filterPermissionAttributes(entityMetaData.getOwnAtomicAttributes());
		}

		@Override
		public AttributeMetaData getIdAttribute()
		{
			return entityMetaData.getIdAttribute();
		}

		@Override
		public AttributeMetaData getOwnIdAttribute()
		{
			return entityMetaData.getOwnIdAttribute();
		}

		@Override
		public AttributeMetaData getLabelAttribute()
		{
			return entityMetaData.getLabelAttribute();
		}

		@Override
		public AttributeMetaData getOwnLabelAttribute()
		{
			return entityMetaData.getOwnLabelAttribute();
		}

		@Override
		public AttributeMetaData getLabelAttribute(String languageCode)
		{
			return entityMetaData.getLabelAttribute(languageCode);
		}

		@Override
		public Iterable<AttributeMetaData> getLookupAttributes()
		{
			return entityMetaData.getLookupAttributes();
		}

		@Override
		public Iterable<AttributeMetaData> getOwnLookupAttributes()
		{
			return entityMetaData.getOwnLookupAttributes();
		}

		@Override
		public AttributeMetaData getLookupAttribute(String attributeName)
		{
			return entityMetaData.getLookupAttribute(attributeName);
		}

		@Override
		public AttributeMetaData getAttribute(String attributeName)
		{
			return entityMetaData.getAttribute(attributeName);
		}

		@Override
		public boolean hasAttributeWithExpression()
		{
			return entityMetaData.hasAttributeWithExpression();
		}

		@Override
		public EntityMetaData getExtends()
		{
			return entityMetaData.getExtends();
		}

		@Override
		public Class<? extends Entity> getEntityClass()
		{
			return entityMetaData.getEntityClass();
		}

		@Override
		public boolean isRowLevelSecured()
		{
			return entityMetaData.isRowLevelSecured();
		}

		private List<AttributeMetaData> filterPermissionAttributes(Iterable<AttributeMetaData> attribute)
		{
			return StreamSupport.stream(entityMetaData.getAttributes().spliterator(), false).filter(attr -> {
				if (ROW_LEVEL_SECURITY_ATTRIBUTES.contains(attr.getName()))
				{
					return SecurityUtils.currentUserIsSu()
							|| SecurityUtils.currentUserHasRole(SystemSecurityToken.ROLE_SYSTEM);
				}
				else
				{
					return true;
				}
			}).collect(Collectors.toList());
		}
	}
}
