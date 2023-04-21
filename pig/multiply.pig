M = LOAD '$M' using PigStorage(',') as (i:int,j:int,value:double);
N = LOAD '$N' using PigStorage(',') as (j:int,k:int,value:double);

joinMN = JOIN M by j, N by j;
multiplyMN = FOREACH joinMN GENERATE $0 as i, $4 as j, ($2*$5) as value;

generateIJ = GROUP multiplyMN by (i,j);
resultantMatrix = foreach generateIJ generate $0.$0 as i, $0.$1 as j, SUM($1.$2) as value;

sortedOrderResult = ORDER resultantMatrix BY i, j;

-- Change output format to "row,col,value"
finalResult = foreach sortedOrderResult generate CONCAT(CONCAT((chararray)i, ','), CONCAT((chararray)j, '')), (chararray)value;

-- Store finalResult into '$O' using PigStorage(',')
store finalResult into '$O' using PigStorage(',');