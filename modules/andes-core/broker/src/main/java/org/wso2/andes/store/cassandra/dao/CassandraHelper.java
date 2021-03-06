/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.andes.store.cassandra.dao;

public class CassandraHelper {
	
	public enum WHERE_OPERATORS{
		EQ,
		LT,
		LTE,
		GT,
		GTE,
		ASC,
		DESC,
		IN,FILTERING,LIMIT;
		
	}
	
	public static class ColumnValueMap{
		private final String column;
		private final Object value;
		private final WHERE_OPERATORS operator;
		
		public ColumnValueMap(String column, Object value, WHERE_OPERATORS operator) {
			super();
			this.column = column;
			this.value = value;
			this.operator = operator;
		}

		public String getColumn() {
			return column;
		}

		public Object getValue() {
			return value;
		}

		public WHERE_OPERATORS getOperator() {
			return operator;
		}
		
		
		
	}

}
