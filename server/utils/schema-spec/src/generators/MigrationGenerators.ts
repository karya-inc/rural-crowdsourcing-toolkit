import { DatabaseSpec } from '../SchemaInterface';
import { knexTableSpec, tableTemplate, typescriptTableName, typescriptTableRecordSpec } from './TableGenerators';

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