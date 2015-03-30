package org.molgenis.data.version.v1_5;

import static org.molgenis.data.support.QueryImpl.EQ;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.sql.DataSource;

import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.molgenis.data.Entity;
import org.molgenis.data.Repository;
import org.molgenis.data.elasticsearch.SearchService;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.meta.MetaDataService;
import org.molgenis.data.meta.MetaDataServiceImpl;
import org.molgenis.data.meta.PackageMetaData;
import org.molgenis.data.meta.TagMetaData;
import org.molgenis.data.meta.migrate.v1_4.AttributeMetaDataMetaData1_4;
import org.molgenis.data.meta.migrate.v1_4.EntityMetaDataMetaData1_4;
import org.molgenis.data.mysql.AsyncJdbcTemplate;
import org.molgenis.data.mysql.MysqlRepository;
import org.molgenis.data.mysql.MysqlRepositoryCollection;
import org.molgenis.data.support.DataServiceImpl;
import org.molgenis.data.version.MetaDataUpgrade;
import org.molgenis.security.runas.RunAsSystemProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileCopyUtils;

import com.google.common.collect.Lists;

/**
 * Upgrades the metadata repositories in MySQL.
 */
public class Step1 extends MetaDataUpgrade
{
	private static final Logger LOG = LoggerFactory.getLogger(Step1.class);
	private JdbcTemplate jdbcTemplate;
	private DataSource dataSource;
	private MysqlRepositoryCollection undecoratedMySQL;
	private SearchService searchService;

	public Step1(DataSource dataSource, SearchService searchService)
	{
		super(0, 1);
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.dataSource = dataSource;
		this.searchService = searchService;
	}

	@Override
	public void upgrade()
	{
		LOG.info("Upgrade MySQL metadata tables...");

		LOG.info("Update metadata table structure...");
		updateMetaDataDatabaseTables();

		LOG.info("Create bare mysql repository collection for the metadata...");
		DataServiceImpl dataService = new DataServiceImpl();
		// Get the undecorated repos
		undecoratedMySQL = new MysqlRepositoryCollection()
		{
			@Override
			protected MysqlRepository createMysqlRepository()
			{
				return new MysqlRepository(dataService, dataSource, new AsyncJdbcTemplate(new JdbcTemplate(dataSource)));
			}

			@Override
			public boolean hasRepository(String name)
			{
				throw new NotImplementedException("Not implemented yet");
			}
		};
		MetaDataService metaData = new MetaDataServiceImpl(dataService);
		RunAsSystemProxy.runAsSystem(() -> metaData.setDefaultBackend(undecoratedMySQL));

		LOG.info("Read attribute order from ElasticSearch and write to the bare mysql repositories...");
		updateAttributesInMysql();

		LOG.info("Reindex metadata repositories...");
		recreateElasticSearchMetaDataIndices();
		LOG.info("Upgrade MySQL metadata tables DONE.");
	}

	private void updateMetaDataDatabaseTables()
	{
		InputStream in = getClass().getResourceAsStream("/2582.sql");
		String script;
		try
		{
			script = FileCopyUtils.copyToString(new InputStreamReader(in));
		}
		catch (IOException e)
		{
			LOG.error("Failed to read upgrade script", e);
			throw new RuntimeException(e);
		}
		for (String statement : script.split(";"))
		{
			String trimmed = statement.trim();
			try
			{
				LOG.info(trimmed);
				jdbcTemplate.execute(trimmed);
			}
			catch (DataAccessException e)
			{
				LOG.error(e.getMessage());
			}

		}
	}

	private void updateAttributesInMysql()
	{
		LOG.info("Update attribute order in MySQL...");
		Repository entityRepository = undecoratedMySQL.getRepository(EntityMetaDataMetaData.ENTITY_NAME);
		// save all entity metadata with attributes in proper order
		for (Entity v15EntityMetaDataEntity : entityRepository)
		{
			LOG.info("Setting attribute order for entity: "
					+ v15EntityMetaDataEntity.get(EntityMetaDataMetaData1_4.SIMPLE_NAME));
			List<Entity> attributes = Lists.newArrayList(searchService.search(
					EQ(AttributeMetaDataMetaData1_4.ENTITY,
							v15EntityMetaDataEntity.getString(EntityMetaDataMetaData.SIMPLE_NAME)),
					new AttributeMetaDataMetaData1_4()));
			v15EntityMetaDataEntity.set(EntityMetaDataMetaData.ATTRIBUTES, attributes);
			v15EntityMetaDataEntity.set(EntityMetaDataMetaData.BACKEND, "MySQL");
			entityRepository.update(v15EntityMetaDataEntity);
		}
		LOG.info("Update attribute order done.");
	}

	private void recreateElasticSearchMetaDataIndices()
	{
		LOG.info("Deleting metadata indices...");
		searchService.delete(EntityMetaDataMetaData.ENTITY_NAME);
		searchService.delete(AttributeMetaDataMetaData.ENTITY_NAME);
		searchService.delete(TagMetaData.ENTITY_NAME);
		searchService.delete(PackageMetaData.ENTITY_NAME);

		searchService.refresh();

		LOG.info("Deleting metadata indices DONE.");

		try
		{
			// sleep just a bit to be sure changes have been persisted
			Thread.sleep(1500);
		}
		catch (InterruptedException e1)
		{
			e1.printStackTrace();
		}

		LOG.info("Adding metadata indices...");

		try
		{
			searchService.createMappings(new TagMetaData());
			searchService.createMappings(new PackageMetaData());
			searchService.createMappings(new AttributeMetaDataMetaData());
			searchService.createMappings(new EntityMetaDataMetaData());
		}
		catch (IOException e)
		{
			LOG.error("error creating metadata mappings", e);
		}

		LOG.info("Reindexing MySQL repositories...");

		searchService.rebuildIndex(undecoratedMySQL.getRepository(TagMetaData.ENTITY_NAME), new TagMetaData());
		searchService.rebuildIndex(undecoratedMySQL.getRepository(PackageMetaData.ENTITY_NAME), new PackageMetaData());
		searchService.rebuildIndex(undecoratedMySQL.getRepository(AttributeMetaDataMetaData.ENTITY_NAME),
				new AttributeMetaDataMetaData());
		searchService.rebuildIndex(undecoratedMySQL.getRepository(EntityMetaDataMetaData.ENTITY_NAME),
				new EntityMetaDataMetaData());

		LOG.info("Reindexing MySQL repositories DONE.");

	}

}
