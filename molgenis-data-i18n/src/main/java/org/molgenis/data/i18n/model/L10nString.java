package org.molgenis.data.i18n.model;

import static org.molgenis.data.i18n.model.L10nStringMetadata.DESCRIPTION;
import static org.molgenis.data.i18n.model.L10nStringMetadata.ID;
import static org.molgenis.data.i18n.model.L10nStringMetadata.MSGID;
import static org.molgenis.data.i18n.model.L10nStringMetadata.NAMESPACE;

import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.molgenis.data.Entity;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.support.StaticEntity;

/** Localization string entity, enabling runtime localization. */
@SuppressWarnings("unused")
public class L10nString extends StaticEntity {
  public L10nString(Entity entity) {
    super(entity);
  }

  public L10nString(EntityType entityType) {
    super(entityType);
  }

  public L10nString(String id, EntityType entityType) {
    super(entityType);
    setId(id);
  }

  public String getId() {
    return getString(ID);
  }

  public L10nString setId(String id) {
    set(ID, id);
    return this;
  }

  public String getMessageID() {
    return getString(MSGID);
  }

  public L10nString setMessageID(String messageID) {
    set(MSGID, messageID);
    return this;
  }

  public String getNamespace() {
    return getString(NAMESPACE);
  }

  public L10nString setNamespace(String namespace) {
    set(NAMESPACE, namespace);
    return this;
  }

  @Nullable
  @CheckForNull
  public String getDescription() {
    return getString(DESCRIPTION);
  }

  public L10nString setDescription(String description) {
    set(DESCRIPTION, description);
    return this;
  }

  public String getString(Locale locale) {
    return getString(locale.getLanguage());
  }
}
