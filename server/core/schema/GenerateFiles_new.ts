// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.
//
// Script to generate files from the specifications.

import { writeTypescriptInterfaceFile, writeTableFunctionsFile, DatabaseSpec, TableSpec } from '@karya/schema-spec';
import { karyaServerDb } from './specs/KaryaDb';
import { karyaServerDb_new } from './specs/KaryaDb_new';
import fs from 'fs';
// import { knex } from '@karya/common';

const TYPES_FOLDER = `${process.cwd()}/src/auto`;
const TABLE_FUNCTIONS_FOLDER = `${process.cwd()}/../common/src/db/auto/`;

// Table interfaces files
const tableInterfacesFile = `${TYPES_FOLDER}/TableInterfaces.ts`;
writeTypescriptInterfaceFile(karyaServerDb_new, '../types/Custom', tableInterfacesFile);

// // Server create table function
// const serverTableFunctionsFile = `${TABLE_FUNCTIONS_FOLDER}/ServerTableFunctions.ts`;
// writeTableFunctionsFile(karyaServerDb, '../Client', serverTableFunctionsFile);

// // Box create table function
// const boxTableFunctionsFile = `${TABLE_FUNCTIONS_FOLDER}/BoxTableFunctions.ts`;
// writeTableFunctionsFile(karyaBoxDb, '../Client', boxTableFunctionsFile);

export function knexTableSpecAddColumn<T extends string, S extends string, O extends string>(
    old_spec: DatabaseSpec<T, S, O>,
    new_spec: DatabaseSpec<T, S, O>
  ): string[] {
    const old_server_tables = old_spec.tables;
    const new_server_tables = new_spec.tables;
    const query = Object.keys(old_server_tables).map((name) => {
        const oldTableSpec = old_server_tables[name as T].columns.map((column) => column[0]);
        const newTableSpec = new_server_tables[name as T].columns.map((column) => column[0]);
        const extraColumns = newTableSpec.filter((column) => {
            return !oldTableSpec.includes(column)
        })
        return extraColumns.length ? `ALTER TABLE ${name} ADD COLUMN ${extraColumns.map(str => `'${str}'`)};` : ""
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

const migrationFileName = `${TABLE_FUNCTIONS_FOLDER}/migrationQueries.ts`;
const migrationQueries = knexTableSpecAddColumn(karyaServerDb, karyaServerDb_new)
let fileString = `import { knex } from '../Client';`
migrationQueries.forEach((query) => {
  fileString += `\n knex.raw(${query})`
})
fs.writeFileSync(migrationFileName, fileString)
// queries.forEach(async (query) => {
//   await knex.raw(query);
// })




