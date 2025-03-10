/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.proxy.backend.handler.distsql.ral.queryable;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.distsql.parser.statement.ral.queryable.ConvertYamlConfigurationStatement;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.datasource.props.DataSourceProperties;
import org.apache.shardingsphere.infra.datasource.props.DataSourcePropertiesCreator;
import org.apache.shardingsphere.infra.datasource.props.custom.CustomDataSourceProperties;
import org.apache.shardingsphere.infra.datasource.props.synonym.PoolPropertySynonyms;
import org.apache.shardingsphere.infra.merge.result.impl.local.LocalDataQueryResultRow;
import org.apache.shardingsphere.infra.util.yaml.YamlEngine;
import org.apache.shardingsphere.infra.yaml.config.pojo.algorithm.YamlAlgorithmConfiguration;
import org.apache.shardingsphere.infra.yaml.config.pojo.rule.YamlRuleConfiguration;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.proxy.backend.config.yaml.YamlProxyDataSourceConfiguration;
import org.apache.shardingsphere.proxy.backend.config.yaml.YamlProxyDatabaseConfiguration;
import org.apache.shardingsphere.proxy.backend.config.yaml.swapper.YamlProxyDataSourceConfigurationSwapper;
import org.apache.shardingsphere.proxy.backend.exception.FileIOException;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.QueryableRALBackendHandler;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.common.constant.DistSQLScriptConstants;
import org.apache.shardingsphere.readwritesplitting.yaml.config.rule.YamlReadwriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.yaml.config.strategy.YamlStaticReadwriteSplittingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.keygen.KeyGenerateStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.ComplexShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.ShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.yaml.config.YamlShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.yaml.swapper.YamlShardingRuleConfigurationSwapper;
import org.apache.shardingsphere.readwritesplitting.yaml.config.YamlReadwriteSplittingRuleConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

/**
 * Convert YAML configuration handler.
 */
public final class ConvertYamlConfigurationHandler extends QueryableRALBackendHandler<ConvertYamlConfigurationStatement> {
    
    private final YamlProxyDataSourceConfigurationSwapper dataSourceConfigSwapper = new YamlProxyDataSourceConfigurationSwapper();
    
    @Override
    protected Collection<String> getColumnNames() {
        return Collections.singleton("distsql");
    }
    
    @Override
    protected Collection<LocalDataQueryResultRow> getRows(final ContextManager contextManager) {
        File file = new File(getSqlStatement().getFilePath());
        YamlProxyDatabaseConfiguration yamlConfig;
        try {
            yamlConfig = YamlEngine.unmarshal(file, YamlProxyDatabaseConfiguration.class);
        } catch (final IOException ex) {
            throw new FileIOException(ex);
        }
        Preconditions.checkNotNull(yamlConfig, "Invalid yaml file `%s`", file.getName());
        Preconditions.checkNotNull(yamlConfig.getDatabaseName(), "`databaseName` in file `%s` is required.", file.getName());
        return Collections.singleton(new LocalDataQueryResultRow(generateDistSQL(yamlConfig)));
    }
    
    private String generateDistSQL(final YamlProxyDatabaseConfiguration yamlConfig) {
        StringBuilder result = new StringBuilder();
        String databaseType = yamlConfig.getDatabaseName();
        switch (databaseType) {
            case DistSQLScriptConstants.RESOURCE_DB:
                addResourceDistSQL(yamlConfig, result);
                break;
            case DistSQLScriptConstants.SHARDING_DB:
                addShardingDistSQL(yamlConfig, result);
                break;
            case DistSQLScriptConstants.READWRITE_SPLITTING_DB:
                addReadWriteSplittingDistSQL(yamlConfig, result);
                break;
            default:
                break;
        }
        return result.toString();
    }
    
    private void addResourceDistSQL(final YamlProxyDatabaseConfiguration yamlConfig, final StringBuilder result) {
        appendDatabase(yamlConfig.getDatabaseName(), result);
        appendResources(yamlConfig.getDataSources(), result);
    }
    
    private void addShardingDistSQL(final YamlProxyDatabaseConfiguration yamlConfig, final StringBuilder result) {
        appendDatabase(yamlConfig.getDatabaseName(), result);
        appendResources(yamlConfig.getDataSources(), result);
        appendShardingRules(yamlConfig.getRules(), result);
    }
    
    private void addReadWriteSplittingDistSQL(final YamlProxyDatabaseConfiguration yamlConfig, final StringBuilder result) {
        appendDatabase(yamlConfig.getDatabaseName(), result);
        appendResources(yamlConfig.getDataSources(), result);
        appendReadWriteSplittingRules(yamlConfig.getRules(), result);
    }
    
    private void appendDatabase(final String databaseName, final StringBuilder result) {
        result.append(String.format(DistSQLScriptConstants.CREATE_DATABASE, databaseName)).append(System.lineSeparator());
        result.append(String.format(DistSQLScriptConstants.USE_DATABASE, databaseName)).append(System.lineSeparator());
    }
    
    private void appendResources(final Map<String, YamlProxyDataSourceConfiguration> dataSources, final StringBuilder result) {
        if (dataSources.isEmpty()) {
            return;
        }
        result.append(DistSQLScriptConstants.ADD_RESOURCE);
        Iterator<Entry<String, YamlProxyDataSourceConfiguration>> iterator = dataSources.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, YamlProxyDataSourceConfiguration> entry = iterator.next();
            DataSourceProperties dataSourceProperties = DataSourcePropertiesCreator.create(HikariDataSource.class.getName(), dataSourceConfigSwapper.swap(entry.getValue()));
            appendResource(entry.getKey(), dataSourceProperties, result);
            if (iterator.hasNext()) {
                result.append(DistSQLScriptConstants.COMMA);
            }
        }
        result.append(DistSQLScriptConstants.SEMI).append(System.lineSeparator());
    }
    
    private void appendResource(final String resourceName, final DataSourceProperties dataSourceProperties, final StringBuilder result) {
        Map<String, Object> connectionProperties = dataSourceProperties.getConnectionPropertySynonyms().getStandardProperties();
        String url = (String) connectionProperties.get(DistSQLScriptConstants.KEY_URL);
        String username = (String) connectionProperties.get(DistSQLScriptConstants.KEY_USERNAME);
        String password = (String) connectionProperties.get(DistSQLScriptConstants.KEY_PASSWORD);
        String props = getResourceProperties(dataSourceProperties.getPoolPropertySynonyms(), dataSourceProperties.getCustomDataSourceProperties());
        if (StringUtils.isNotEmpty(password)) {
            result.append(String.format(DistSQLScriptConstants.RESOURCE_DEFINITION, resourceName, url, username, password, props));
        } else {
            result.append(String.format(DistSQLScriptConstants.RESOURCE_DEFINITION_WITHOUT_PASSWORD, resourceName, url, username, props));
        }
    }
    
    private String getResourceProperties(final PoolPropertySynonyms poolPropertySynonyms, final CustomDataSourceProperties customDataSourceProperties) {
        StringBuilder result = new StringBuilder();
        appendProperties(poolPropertySynonyms.getStandardProperties(), result);
        if (!customDataSourceProperties.getProperties().isEmpty()) {
            result.append(DistSQLScriptConstants.COMMA);
            appendProperties(customDataSourceProperties.getProperties(), result);
        }
        return result.toString();
    }
    
    private void appendProperties(final Map<String, Object> properties, final StringBuilder stringBuilder) {
        Iterator<Entry<String, Object>> iterator = properties.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Object> entry = iterator.next();
            if (null == entry.getValue()) {
                continue;
            }
            stringBuilder.append(String.format(DistSQLScriptConstants.PROPERTY, entry.getKey(), entry.getValue()));
            if (iterator.hasNext()) {
                stringBuilder.append(DistSQLScriptConstants.COMMA);
            }
        }
    }
    
    private void appendShardingRules(final Collection<YamlRuleConfiguration> ruleConfigs, final StringBuilder result) {
        if (ruleConfigs.isEmpty()) {
            return;
        }
        for (YamlRuleConfiguration ruleCofig : ruleConfigs) {
            ShardingRuleConfiguration shardingRuleConfig = new YamlShardingRuleConfigurationSwapper().swapToObject((YamlShardingRuleConfiguration) ruleCofig);
            appendShardingAlgorithms(shardingRuleConfig, result);
            appendKeyGenerators(shardingRuleConfig, result);
            appendShardingTableRules(shardingRuleConfig, result);
            // TODO append autoTables
            appendShardingBindingTableRules(shardingRuleConfig, result);
        }
    }
    
    private void appendShardingAlgorithms(final ShardingRuleConfiguration shardingRuleConfig, final StringBuilder result) {
        result.append(DistSQLScriptConstants.CREATE_SHARDING_ALGORITHM);
        Iterator<Entry<String, AlgorithmConfiguration>> iterator = shardingRuleConfig.getShardingAlgorithms().entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, AlgorithmConfiguration> entry = iterator.next();
            String shardingAlgorithmName = entry.getKey();
            String algorithmType = entry.getValue().getType().toLowerCase();
            String property = appendShardingAlgorithmProperties(entry.getValue().getProps());
            result.append(String.format(DistSQLScriptConstants.SHARDING_ALGORITHM, shardingAlgorithmName, algorithmType, property));
            if (iterator.hasNext()) {
                result.append(DistSQLScriptConstants.COMMA);
            }
        }
        result.append(DistSQLScriptConstants.SEMI).append(System.lineSeparator());
    }
    
    private String appendShardingAlgorithmProperties(final Properties property) {
        StringBuilder result = new StringBuilder();
        Iterator<Entry<Object, Object>> iterator = property.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Object, Object> entry = iterator.next();
            result.append(String.format(DistSQLScriptConstants.PROPERTY, entry.getKey(), entry.getValue()));
            if (iterator.hasNext()) {
                result.append(DistSQLScriptConstants.COMMA);
            }
        }
        return result.toString();
    }
    
    private void appendKeyGenerators(final ShardingRuleConfiguration shardingRuleConfig, final StringBuilder result) {
        result.append(DistSQLScriptConstants.CREATE_KEY_GENERATOR);
        Iterator<Entry<String, AlgorithmConfiguration>> iterator = shardingRuleConfig.getKeyGenerators().entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, AlgorithmConfiguration> entry = iterator.next();
            String generatorName = entry.getKey();
            String type = entry.getValue().getType();
            result.append(String.format(DistSQLScriptConstants.KEY_GENERATOR, generatorName, type));
            if (iterator.hasNext()) {
                result.append(DistSQLScriptConstants.COMMA);
            }
        }
        result.append(DistSQLScriptConstants.SEMI).append(System.lineSeparator());
    }
    
    private void appendShardingTableRules(final ShardingRuleConfiguration shardingRuleConfig, final StringBuilder result) {
        result.append(DistSQLScriptConstants.CREATE_SHARDING_TABLE);
        Iterator<ShardingTableRuleConfiguration> iterator = shardingRuleConfig.getTables().iterator();
        while (iterator.hasNext()) {
            ShardingTableRuleConfiguration entry = iterator.next();
            String tableName = entry.getLogicTable();
            String dataNodes = entry.getActualDataNodes();
            String strategy = appendTableStrategy(entry);
            result.append(String.format(DistSQLScriptConstants.SHARDING_TABLE, tableName, dataNodes, strategy));
            if (iterator.hasNext()) {
                result.append(DistSQLScriptConstants.COMMA);
            }
        }
        result.append(DistSQLScriptConstants.SEMI).append(System.lineSeparator());
    }
    
    private String appendTableStrategy(final ShardingTableRuleConfiguration shardingTableRuleConfig) {
        StringBuilder result = new StringBuilder();
        if (null != shardingTableRuleConfig.getDatabaseShardingStrategy()) {
            getStrategy(shardingTableRuleConfig.getDatabaseShardingStrategy(), DistSQLScriptConstants.DATABASE_STRATEGY, result);
        }
        if (null != shardingTableRuleConfig.getTableShardingStrategy()) {
            getStrategy(shardingTableRuleConfig.getTableShardingStrategy(), DistSQLScriptConstants.TABLE_STRATEGY, result);
        }
        if (null != shardingTableRuleConfig.getKeyGenerateStrategy()) {
            KeyGenerateStrategyConfiguration keyGenerateStrategyConfig = shardingTableRuleConfig.getKeyGenerateStrategy();
            String column = keyGenerateStrategyConfig.getColumn();
            String keyGenerator = keyGenerateStrategyConfig.getKeyGeneratorName();
            result.append(String.format(DistSQLScriptConstants.KEY_GENERATOR_STRATEGY, column, keyGenerator));
        }
        return result.substring(0, result.length() - 2);
    }
    
    private StringBuilder getStrategy(final ShardingStrategyConfiguration shardingStrategyConfiguration, final String strategyType, final StringBuilder result) {
        String type = shardingStrategyConfiguration.getType().toLowerCase();
        String shardingAlgorithmName = shardingStrategyConfiguration.getShardingAlgorithmName();
        switch (type) {
            case DistSQLScriptConstants.STANDARD:
                StandardShardingStrategyConfiguration standardShardingStrategyConfig = (StandardShardingStrategyConfiguration) shardingStrategyConfiguration;
                String shardingColumn = standardShardingStrategyConfig.getShardingColumn();
                result.append(String.format(DistSQLScriptConstants.SHARDING_STRATEGY_STANDARD, strategyType, type, shardingColumn, shardingAlgorithmName));
                break;
            case DistSQLScriptConstants.COMPLEX:
                ComplexShardingStrategyConfiguration complexShardingStrategyConfig = (ComplexShardingStrategyConfiguration) shardingStrategyConfiguration;
                String shardingColumns = complexShardingStrategyConfig.getShardingColumns();
                result.append(String.format(DistSQLScriptConstants.SHARDING_STRATEGY_COMPLEX, strategyType, type, shardingColumns, shardingAlgorithmName));
                break;
            case DistSQLScriptConstants.HINT:
                result.append(String.format(DistSQLScriptConstants.SHARDING_STRATEGY_HINT, type, shardingAlgorithmName));
                break;
            default:
                break;
        }
        return result;
    }
    
    private void appendShardingBindingTableRules(final ShardingRuleConfiguration shardingRuleConfig, final StringBuilder result) {
        String bindings = getBindings(shardingRuleConfig.getBindingTableGroups().iterator());
        result.append(String.format(DistSQLScriptConstants.SHARDING_BINDING_TABLE_RULES, bindings));
    }
    
    private String getBindings(final Iterator<String> iterator) {
        StringBuilder result = new StringBuilder();
        while (iterator.hasNext()) {
            String binding = iterator.next();
            result.append(String.format(DistSQLScriptConstants.BINDING, binding));
            if (iterator.hasNext()) {
                result.append(DistSQLScriptConstants.COMMA);
            }
        }
        result.append(DistSQLScriptConstants.SEMI).append(System.lineSeparator());
        return result.toString();
    }
    
    private void appendReadWriteSplittingRules(final Collection<YamlRuleConfiguration> ruleConfigs, final StringBuilder result) {
        if (ruleConfigs.isEmpty()) {
            return;
        }
        result.append(DistSQLScriptConstants.CREATE_READWRITE_SPLITTING_RULE);
        for (YamlRuleConfiguration ruleConfig : ruleConfigs) {
            appendStaticReadWriteSplittingRule(ruleConfig, result);
            // TODO Dynamic READ-WRITE-SPLITTING RULES
        }
    }
    
    private void appendStaticReadWriteSplittingRule(final YamlRuleConfiguration ruleConfig, final StringBuilder result) {
        Iterator<Entry<String, YamlReadwriteSplittingDataSourceRuleConfiguration>> dataSources = ((YamlReadwriteSplittingRuleConfiguration) ruleConfig).getDataSources().entrySet().iterator();
        Iterator<Entry<String, YamlAlgorithmConfiguration>> loadBalancers = ((YamlReadwriteSplittingRuleConfiguration) ruleConfig).getLoadBalancers().entrySet().iterator();
        while (dataSources.hasNext()) {
            Entry<String, YamlReadwriteSplittingDataSourceRuleConfiguration> entryDataSources = dataSources.next();
            Entry<String, YamlAlgorithmConfiguration> entryLoadBalances = loadBalancers.next();
            YamlStaticReadwriteSplittingStrategyConfiguration staticStrategy = entryDataSources.getValue().getStaticStrategy();
            String dataSourceName = entryDataSources.getKey();
            String writeDataSourceName = staticStrategy.getWriteDataSourceName();
            String readDataSourceNames = appendReadDataSourceNames(staticStrategy.getReadDataSourceNames());
            String loadBalancerType = appendLoadBalancer(entryDataSources.getValue().getLoadBalancerName(), entryLoadBalances);
            result.append(String.format(DistSQLScriptConstants.STATIC_READWRITE_SPLITTING, dataSourceName, writeDataSourceName, readDataSourceNames, loadBalancerType));
            if (dataSources.hasNext()) {
                result.append(DistSQLScriptConstants.COMMA);
            }
        }
        result.append(DistSQLScriptConstants.SEMI).append(System.lineSeparator());
    }
    
    private String appendReadDataSourceNames(final Collection<String> readDataSourceNames) {
        StringBuilder result = new StringBuilder();
        Iterator<String> iterator = readDataSourceNames.iterator();
        while (iterator.hasNext()) {
            String readDataSourceName = iterator.next();
            result.append(String.format(DistSQLScriptConstants.READ_RESOURCE, readDataSourceName));
            if (iterator.hasNext()) {
                result.append(DistSQLScriptConstants.COMMA);
            }
        }
        return result.toString();
    }
    
    private String appendLoadBalancer(final String loadBalancerName, final Entry<String, YamlAlgorithmConfiguration> loadBalancers) {
        StringBuilder result = new StringBuilder();
        String loadBalancerProperties = "";
        if (loadBalancers.getValue().getProps().isEmpty()) {
            result.append(String.format(DistSQLScriptConstants.TYPE, loadBalancers.getValue().getType()));
        } else {
            Iterator<Entry<Object, Object>> iterator = loadBalancers.getValue().getProps().entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<Object, Object> entry = iterator.next();
                if (loadBalancerName == entry.getKey()) {
                    loadBalancerProperties = appendLoadBalancerProperties(loadBalancers.getValue().getProps());
                }
            }
            result.append(String.format(DistSQLScriptConstants.TYPE_PROPERTIES, loadBalancers.getValue().getType(), loadBalancerProperties));
        }
        return result.toString();
    }
    
    private String appendLoadBalancerProperties(final Properties loadBalancerProperties) {
        StringBuilder result = new StringBuilder();
        Iterator<Entry<Object, Object>> iterator = loadBalancerProperties.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Object, Object> entry = iterator.next();
            result.append(String.format(DistSQLScriptConstants.PROPERTY, entry.getKey(), entry.getValue()));
            while (iterator.hasNext()) {
                result.append(DistSQLScriptConstants.COMMA);
            }
        }
        return result.toString();
    }
}
