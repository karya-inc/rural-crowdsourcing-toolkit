// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.
//
// Script to generate files from the specifications.

import { writeTypescriptInterfaceFile, DatabaseSpec, writeMigrationFile, TableSpec, writeTableMigrationFile, TableColumnSpec } from '@karya/schema-spec';
import { karyaServerDb } from './specs/KaryaDb';
import { karyaServerDb_new } from './specs/KaryaDb_new';
import { knexColumnSpec } from '@karya/schema-spec/dist/generators/ColumnGenerators';
import camelcase from 'camelcase';

const TYPES_FOLDER = `${process.cwd()}/src/auto`;

// Table interfaces files 
const tableInterfacesFile = `${TYPES_FOLDER}/TableInterfaces.ts`;
writeTypescriptInterfaceFile(karyaServerDb_new, '../types/Custom', tableInterfacesFile);
export function knexTableSpecAddColumn<T extends string, S extends string, O extends string, T1 extends string, S1 extends string, O1 extends string>(
    old_spec: DatabaseSpec<T, S, O>,
    new_spec: DatabaseSpec<T1, S1, O1>
  ): string[] {
    const old_server_tables = old_spec.tables;
    const new_server_tables = new_spec.tables;
    var affectedTables : Array<string> = [];
    const query = Object.keys(old_server_tables).map((name) => {
      
        const oldTableSpec = old_server_tables[name as T].columns;
        const newTableSpec = new_server_tables[name as T1].columns;
        const extraColumns: TableColumnSpec<T1,S1,O1>[] = [];

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
            if(!(affectedTables.includes(name))){
              affectedTables.push(name);
            }
          }
        }
        const tsTableName = typescriptTableName(name);
        const knexColSpecs = extraColumns.map((column) => knexColumnSpec(column));
        return extraColumns?.length ? `
        export async function alterTable${tsTableName}AddColumns() {
        return knex.schema.alterTable('${tsTableName}', async (table) => {
          ${knexColSpecs.join('\n')}
        });
      }` : ""
    })
    var content = query.filter(item => item.length);
    var sub = `export async function createAllMigrationsOfAddColumns() {`;
    Object.keys(old_server_tables).forEach((name) => {
      const tsTableName = typescriptTableName(name);
      if(affectedTables.includes(name)){
        sub+=`await alterTable${tsTableName}AddColumns(); `
      }
    })
    content.push(sub + "}");
    return content;
  }

// only works for those columns which are optional and can be dropped. Will fail for if following columns are dropped : access_code, email, full_name, phone_number, role
export function knexTableSpecDropColumn<T extends string, S extends string, O extends string, T1 extends string, S1 extends string, O1 extends string>(
    old_spec: DatabaseSpec<T, S, O>,
    new_spec: DatabaseSpec<T1, S1, O1>
  ): string [] {
    const old_server_tables = old_spec.tables;
    const new_server_tables = new_spec.tables;
    var affectedTables : Array<string> = [];
    const query = Object.keys(old_server_tables).map((name) => {
      
        const oldTableSpec = old_server_tables[name as T].columns;
        const newTableSpec = new_server_tables[name as T1].columns;
        const extraColumns: TableColumnSpec<T,S,O>[] = [];

        for (var i = 0; i<oldTableSpec.length; i++){
          var flag = false;
          for (var j = 0; j<newTableSpec.length; j++){
            if(newTableSpec[j][0].includes(oldTableSpec[i][0]) && oldTableSpec[i][1][0] === newTableSpec[j][1][0]){
              flag = true;
              break;
            }
          }
          if(flag == false){
            extraColumns.push(oldTableSpec[i])
            if(!(affectedTables.includes(name))){
              affectedTables.push(name);
            }
          }
        }
        const tsTableName = typescriptTableName(name);
        const knexColSpecs = extraColumns.map((column) => `table.dropColumn('${column[0]}')`);
        return extraColumns?.length ? `
        export async function alterTable${tsTableName}DropColumns() {
        await knex.schema.alterTable('${tsTableName}', async (table) => {
          ${knexColSpecs.join('\n')}
        });
      }` : ""
    })
    var content = query.filter(item => item.length);
    var sub = `export async function createAllMigrationsOfDropColumns() {`;
    Object.keys(old_server_tables).forEach((name) => {
      const tsTableName = typescriptTableName(name);
      if(affectedTables.includes(name)){
        sub+=`await alterTable${tsTableName}DropColumns(); `
      }
    })
    content.push(sub + "}");
    return content;
  }

export function knexTableSpecAddTable<T extends string, S extends string, O extends string, T1 extends string, S1 extends string, O1 extends string>(
    old_spec: DatabaseSpec<T, S, O>,
    new_spec: DatabaseSpec<T1, S1, O1>
  ): {[key:string]: TableSpec<T1, S1, O1>} {
    const old_server_tables = old_spec.tables;
    const new_server_tables = new_spec.tables;
    const extraTables : {[key:string]: TableSpec<T1, S1, O1>} = {};
    Object.keys(new_server_tables).map((name:string) => {
        if(!(name in old_server_tables)){
          extraTables[name] = new_server_tables[name as T1];
        }
  }
  )
return extraTables;
}

/**
 * Generate the typescript name for a table. Converts given name to pascal case.
 * @param name Name of the table
 * @returns Typescript name for the table
 */
export function typescriptTableName(name: string): string {
  return camelcase(name, { pascalCase: true });
}

const migrationFileName = `${process.cwd()}/../common/src/db/auto/DataMigrationFunctions.ts`;
const migrationFileNameAddTable = `${process.cwd()}/../common/src/db/auto/DataMigrationFunctionsAddTable.ts`;
const migrationQueriesAddColumn = knexTableSpecAddColumn(karyaServerDb, karyaServerDb_new);
const migrationQueriesDropColumn = knexTableSpecDropColumn(karyaServerDb, karyaServerDb_new);
const migrationQueriesAddTableColumn = knexTableSpecAddTable(karyaServerDb, karyaServerDb_new);
writeMigrationFile(migrationFileName, migrationQueriesAddColumn.concat(migrationQueriesDropColumn), '../Client');
writeTableMigrationFile(migrationFileNameAddTable, migrationQueriesAddTableColumn, '../Client');



