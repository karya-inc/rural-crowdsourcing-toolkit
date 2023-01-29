// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.
//
// Script to generate files from the specifications.

import { writeTypescriptInterfaceFile, DatabaseSpec, writeMigrationFile, TableColumnSpec, TableSpec } from '@karya/schema-spec';
import { karyaServerDb } from './specs/KaryaDb';
import { karyaServerDb_new } from './specs/KaryaDb_new';
import { knexColumnSpec } from '@karya/schema-spec/dist/generators/ColumnGenerators';

const TYPES_FOLDER = `${process.cwd()}/src/auto`;
const TABLE_FUNCTIONS_FOLDER = `${process.cwd()}/../common/src/db/auto/`;

// Table interfaces files 
const tableInterfacesFile = `${TYPES_FOLDER}/TableInterfaces.ts`;
writeTypescriptInterfaceFile(karyaServerDb_new, '../types/Custom', tableInterfacesFile);
export function knexTableSpecAddColumn<T extends string, S extends string, O extends string>(
    old_spec: DatabaseSpec<T, S, O>,
    new_spec: DatabaseSpec<T, S, O>
  ): string[] {
    const old_server_tables = old_spec.tables;
    const new_server_tables = new_spec.tables;
    const query = Object.keys(old_server_tables).map((name) => {
      
        const oldTableSpec = old_server_tables[name as T].columns;
        const newTableSpec = new_server_tables[name as T].columns;
        const extraColumns: TableColumnSpec<T,S,O>[] = [];

        for (var i = 0; i<newTableSpec.length; i++){
          var flag = false;
          for (var j = 0; j<oldTableSpec.length; j++){
            if(oldTableSpec[j][0].includes(newTableSpec[i][0]) && newTableSpec[i][1][0] === oldTableSpec[j][1][0]){
              flag = true;
              break;
            }
          }
        if(flag == false){
          extraColumns.push(newTableSpec[i])
        }
        }
        const knexColSpecs = extraColumns.map((column) => knexColumnSpec(column));
        return extraColumns?.length ? `
        export async function alterTableColumns${name}() {
        knex.schema.alterTable('${name}', async (table) => {
          ${knexColSpecs.join('\n')}
        });
      }` : ""
    })
    return query.filter(item => item.length);
  }
 function knexTableSpecAddTable<T extends string, S extends string, O extends string>(
    old_spec: DatabaseSpec<T, S, O>,
    new_spec: DatabaseSpec<T, S, O>
  ): string [] {
    const old_server_tables = old_spec.tables;
    const new_server_tables = new_spec.tables;
    const query = Object.keys(new_server_tables).filter((name) => !(name in old_server_tables)).map((tableName) => `CREATE TABLE ${tableName} (${new_server_tables[tableName as T]})`);
    return query;
  }

const migrationFileName = `${process.cwd()}/../common/src/db/auto/DataMigrationFunctions.ts`;
const migrationQueries = knexTableSpecAddColumn(karyaServerDb, karyaServerDb_new);
writeMigrationFile(migrationFileName, migrationQueries, '../Client');




