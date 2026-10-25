"""Check English/PT-BR resource coverage, formatting arguments and protocol keys."""
from pathlib import Path
import re,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
def read(folder):
 rows=ET.parse(root/'app/src/main/res'/folder/'translations.xml').getroot()
 values={row.attrib['name']:row.text or '' for row in rows}
 assert len(values)==len(rows),'Duplicate translation keys'
 return values
en,pt=read('values'),read('values-pt-rBR')
assert en.keys()==pt.keys(),'Language coverage differs'
for key in en:
 assert en[key].strip() and pt[key].strip(),key
 assert sorted(re.findall(r'%\d+\$[sd]',en[key]))==sorted(re.findall(r'%\d+\$[sd]',pt[key])),key
 assert '\ufffd' not in en[key]+pt[key],key
for p in (root/'app/src/main/java').rglob('*.kt'):
 s=p.read_text(encoding='utf-8')
 assert not re.search(r'\.(?:opt|get|put)(?:String|Double|Long|Int|Boolean)?\(L10n\.text',s),f'Localized protocol key: {p}'
print(f'{len(en)} translations validated in both languages; protocol keys preserved')
