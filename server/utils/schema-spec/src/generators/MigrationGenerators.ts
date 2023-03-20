import { TableSpec } from '../SchemaInterface';
import { knexTableSpec } from './TableGenerators';

export function knexMigrationSpec<T extends string, S extends string, O extends string>(
    query: string[],
    knexClientPath: string
  ): string {
    var importStatement = `import { knex } from '${knexClientPath}';`
    return `
    ${importStatement}
    ${query.join('\n')}
    `
  }

  export function knexTableMigrationSpec<T extends string, S extends string, O extends string>(
    query: {[key:string]: TableSpec<T, S, O>},
    knexClientPath: string,
  ): string {
    const newTables = Object.keys(query);
    const knexTableSpecs = newTables.map((tableName) => {
      return knexTableSpec(tableName, query[tableName]);
    });

    return `
  import { knex } from '${knexClientPath}';

  // Table functions
  ${knexTableSpecs.join('\n')}
  `
  }