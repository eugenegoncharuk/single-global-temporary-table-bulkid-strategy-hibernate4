import org.hibernate.cfg.Mappings;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.hql.spi.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.TableBasedDeleteHandlerImpl;
import org.hibernate.hql.spi.TableBasedUpdateHandlerImpl;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.SelectValues;
import org.hibernate.type.StringType;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * A strategy resembling {@link GlobalTemporaryTableBulkIdStrategy} modified to use a single "global temporary table" created beforehand (e.g.
 * HT_GLOBAL_TEMP_TABLE_IDS (ID CHAR(36), ENTITY_NAME VARCHAR(100)))
 * <p>
 * Can be useful in environments where DDL statements cannot be executed from application and managing a large number of ID tables is not practical.
 * <p>
 * <b>Note:</b> multicolumn IDs or inconsistent ID types were not tested and will likely NOT work.
 */
public class SingleGlobalTemporaryTableBulkIdStrategy implements MultiTableBulkIdStrategy {

    private static String fullyQualifiedTableName = "HT_GLOBAL_TEMP_TABLE_IDS";
    private static String idColumn = "ID";
    private static String discriminatorColumn = "ENTITY_NAME";
    private static boolean cleanRows = false;

    @Override
    public void release(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {
        // nothing to do
    }

    @Override
    public void prepare(JdbcServices jdbcServices, JdbcConnectionAccess jdbcConnectionAccess, Mappings mappings, Mapping mapping, Map map) {
    }

    @Override
    public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
        final UpdateStatement updateStatement = (UpdateStatement) walker.getAST();
        final Queryable targetedPersister = updateStatement.getFromClause().getFromElement().getQueryable();

        return new TableBasedUpdateHandlerImpl(factory, walker) {
            @Override
            protected String generateIdSubselect(Queryable persister) {
                return getTempTableIdSubselect(targetedPersister);
            }

            @Override
            protected void addAnyExtraIdSelectValues(SelectValues selectClause) {
                addExtraIdSelectValues(targetedPersister, selectClause);
            }

            @Override
            protected void releaseFromUse(Queryable persister, SessionImplementor session) {
                if (cleanRows) {
                    cleanUpRows(session, targetedPersister);
                }
            }

            @Override
            protected String determineIdTableName(Queryable persister) {
                return fullyQualifiedTableName;
            }

        };
    }

    @Override
    public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
        final DeleteStatement deleteStatement = (DeleteStatement) walker.getAST();
        final Queryable targetedPersister = deleteStatement.getFromClause().getFromElement().getQueryable();

        return new TableBasedDeleteHandlerImpl(factory, walker) {

            @Override
            protected String generateIdSubselect(Queryable persister) {
                return getTempTableIdSubselect(targetedPersister);
            }

            @Override
            protected void addAnyExtraIdSelectValues(SelectValues selectClause) {
                addExtraIdSelectValues(targetedPersister, selectClause);
            }

            @Override
            protected void releaseFromUse(Queryable persister, SessionImplementor session) {
                if (cleanRows) {
                    cleanUpRows(session, persister);
                }
            }

            @Override
            protected String determineIdTableName(Queryable persister) {
                return fullyQualifiedTableName;
            }
        };
    }

    private void cleanUpRows(SessionImplementor session, Queryable persister) {
        final String sql = "delete from " + fullyQualifiedTableName + " where " + discriminatorColumn + "=?";
        PreparedStatement ps = null;
        try {
            ps = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement(sql, false);
            ps.setString(1, generateDiscriminatorValue(persister));
            StringType.INSTANCE.set(ps, generateDiscriminatorValue(persister), 1, session);
            session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate(ps);
        } catch (SQLException e) {
            throw session.getTransactionCoordinator().getJdbcCoordinator().getLogicalConnection().getJdbcServices()
                    .getSqlExceptionHelper().convert(e, "Unable to clean up id table [" + fullyQualifiedTableName + "]", sql);
        } finally {
            if (ps != null) {
                //session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release(ps);
            }
        }
    }

    protected String generateDiscriminatorValue(Queryable persister) {
        return persister.getEntityName();
    }

    protected String getTempTableIdSubselect(Queryable persister) {
        return "select " + idColumn
                + " from " + fullyQualifiedTableName
                + " where " + discriminatorColumn + "='" + generateDiscriminatorValue(persister) + "'";
    }

    protected void addExtraIdSelectValues(final Queryable targetedPersister, SelectValues selectClause) {
        selectClause.addColumn(null, '\'' + generateDiscriminatorValue(targetedPersister) + '\'', discriminatorColumn);
    }

    private String getTableName() {
        return fullyQualifiedTableName;
    }
}