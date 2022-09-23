import {expect, test} from '@jest/globals';
import {ArgumentParser} from '../src/codetl/args';

test('Valid CLI options are pulled', () => {
    const parser = new ArgumentParser();
    const parsedArgs = parser.parse(["--repository=/tmp/foo","--verbose=true", "--output=/tmp/foo.codetl"]);
    expect(parsedArgs.verbose).toBe(true);
    expect(parsedArgs.repository).toEqual("/tmp/foo");
});