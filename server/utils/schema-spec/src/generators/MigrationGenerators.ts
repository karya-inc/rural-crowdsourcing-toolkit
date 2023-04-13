import { TableSpec } from '../SchemaInterface';
import { knexAddTableSpec, knexDropTableSpec, typescriptTableName } from './TableGenerators';

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
    intent: string,
    query: {[key:string]: TableSpec<T, S, O>},
    knexClientPath: string,
  ): string {
    const newTables = Object.keys(query);
    if (intent == "addTable"){
      const knexTableSpecs = newTables.map((tableName) => {
        return knexAddTableSpec(tableName, query[tableName]);
      });
      const contentAddTable = []
      var subAddTable = `export async function createAllMigrationsOfAddTable() {`;
  
      newTables.forEach((name) => {
        const tableType = typescriptTableName(name);
          subAddTable+=`await create${tableType}Table(); `
        
      })
      contentAddTable.push(subAddTable + "}");
      return `
    import { knex } from '${knexClientPath}';
    // Table functions
    ${knexTableSpecs.join('\n')}
    ${contentAddTable.join('\n')}
    `
    }
    else {
      const knexTableSpecs = newTables.map((tableName) => {
        return knexDropTableSpec(tableName, query[tableName]);
      });
      const contentDropTable = []
      var subDropTable = `export async function createAllMigrationsOfDropTable() {`;
  
      newTables.forEach((name) => {
        const tableType = typescriptTableName(name);
          subDropTable+=`await drop${tableType}Table(); `
        
      })
      contentDropTable.push(subDropTable + "}");
      return `
    import { knex } from '${knexClientPath}';
    // Table functions
    ${knexTableSpecs.join('\n')}
    ${contentDropTable.join('\n')}
    `
    }
    
  }