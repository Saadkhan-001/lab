import pandas as pd
import json
import os
import re

def parse_range(r):
    if not r or pd.isna(r) or str(r).strip() in ['\u2014', 'N/A', '-', '', 'nan']:
        return "", ""
    r = str(r).replace('\u2013', '-').replace('\u2014', '-').strip()
    r = r.replace('\ufffd', '')
    if '-' in r:
        parts = r.split('-', 1)
        return parts[0].strip(), parts[1].strip()
    return r, ""

def sync_lab_data():
    # Use relative paths for better portability on different workstations
    base_dir = os.path.dirname(os.path.abspath(__file__))
    folder = os.path.join(base_dir, 'tests')
    
    if not os.path.exists(folder):
        print(f"Error: Protocol folder not discovered at: {folder}")
        return
        
    all_tests = []
    xlsx_files = sorted([f for f in os.listdir(folder) if f.endswith('.xlsx')])
    
    for filename in xlsx_files:
        path = os.path.join(folder, filename)
        print(f"Processing clinical protocol: {filename}...")
        try:
            all_sheets = pd.read_excel(path, sheet_name=None)
        except Exception as e:
            print(f"Error reading {filename}: {e}")
            continue
            
        for sheet_name, df in all_sheets.items():
            df = df.fillna("")
            current_test = None
            in_params = False
            header_map = {}
            
            for idx, row in df.iterrows():
                row_vals = [str(x).strip() for x in row.values]
                row_str = " ".join(row_vals)
                row_str_up = row_str.upper()
                
                test_match = re.search(r'\[(\d+)\]\s+([A-Z0-9/\-\+]+)\s+[\-|\u2014]\s+(.*?)(?:\s+\||$)', row_str)
                if not test_match:
                    test_match = re.search(r'\[(\d+)\]\s+(.*?)(?:\s+\||$)', row_str)
                
                if test_match:
                    if current_test: all_tests.append(current_test)
                    
                    code = test_match.group(1).strip()
                    raw_rem = test_match.group(2).strip()
                    alpha_code = ""
                    clean_name = raw_rem
                    
                    if len(test_match.groups()) >= 3:
                        alpha_code = test_match.group(2).strip()
                        clean_name = test_match.group(3).split('|')[0].strip()
                    elif " - " in raw_rem:
                        parts = raw_rem.split(" - ", 1)
                        alpha_code = parts[0].strip()
                        clean_name = parts[1].split('|')[0].strip()
                    
                    alpha_code = alpha_code.split('|')[0].strip()
                    
                    current_test = {
                        "code": code,
                        "alpha_code": alpha_code,
                        "name": clean_name,
                        "category": "Routine",
                        "price": 0, "result_time": "24 hrs", "specimen": "Blood",
                        "parameters": [],
                        "notes": "",
                        "is_culture": 1 if "Culture" in clean_name else 0,
                        "is_microscopic": 1 if any(x in clean_name for x in ["Routine", "Analysis", "Microscopic", "Stool", "Semen", "Urine RE"]) else 0,
                        "is_special": 1 if any(x in clean_name for x in ["PCR", "Mutation", "Vitamin", "Bio", "HbA1c"]) else 0
                    }
                    
                    price_match = re.search(r'Price:\s*(?:Rs\s*)?([\d,]+)', row_str)
                    if price_match: current_test["price"] = int(price_match.group(1).replace(',', ''))
                    tat_match = re.search(r'TAT:\s*(.*?)(?:\s*\||$)', row_str)
                    if tat_match: current_test["result_time"] = tat_match.group(1).strip()
                    dept_match = re.search(r'Dept:\s*(.*?)(?:\s*\||$)', row_str)
                    if dept_match: current_test["category"] = dept_match.group(1).strip()
                    in_params = False
                    continue
                
                if current_test and "Specimen:" in row_str:
                    spec_match = re.search(r'Specimen:\s*(.*?)(?:\s*\||$)', row_str)
                    if spec_match: current_test["specimen"] = spec_match.group(1).strip()
                    continue
                
                if "PARAMETER" in row_str_up and "UNIT" in row_str_up:
                    in_params = True
                    header_map = {}
                    for i, v in enumerate(row_vals):
                        vu = v.upper()
                        if "PARAMETER" in vu or "INVESTIGATION" in vu: header_map["P"] = i
                        elif "UNIT" in vu: header_map["U"] = i
                        elif "MALE" in vu and ("NORMAL" in vu or "RANGE" in vu): header_map["M"] = i
                        elif "FEMALE" in vu and ("NORMAL" in vu or "RANGE" in vu): header_map["F"] = i
                        elif ("PAED" in vu or "KID" in vu or "CHILD" in vu) and ("NORMAL" in vu or "RANGE" in vu): header_map["K"] = i
                    continue
                
                if in_params and current_test and "P" in header_map:
                    p_idx = header_map["P"]
                    param_name = row_vals[p_idx]
                    
                    if not param_name or "[" in param_name:
                        if not param_name: continue
                        in_params = False
                    else:
                        u_idx = header_map.get("U", p_idx + 1)
                        m_idx = header_map.get("M", u_idx + 1)
                        f_idx = header_map.get("F", m_idx + 1)
                        k_idx = header_map.get("K", f_idx + 1)
                        
                        unit = row_vals[u_idx] if u_idx < len(row_vals) else "-"
                        unit = unit.replace('\ufffd', '').replace('10/', '\u00d710\u00b3/')
                        
                        male_r = row_vals[m_idx] if m_idx < len(row_vals) else ""
                        fem_r = row_vals[f_idx] if f_idx < len(row_vals) else ""
                        kids_r = row_vals[k_idx] if k_idx < len(row_vals) else ""
                        
                        min_m, max_m = parse_range(male_r)
                        min_f, max_f = parse_range(fem_r)
                        min_k, max_k = parse_range(kids_r)
                        
                        dr = "-"
                        for rr in [male_r, fem_r, kids_r]:
                            if rr and rr not in ["-", "\u2014", "N/A"]:
                                dr = rr; break
                        min_def, max_def = parse_range(dr)

                        current_test["parameters"].append({
                            "name": param_name,
                            "unit": unit if unit not in ["\u2014", ""] else "-",
                            "range": dr,
                            "min_range": min_def, "max_range": max_def,
                            "min_range_male": min_m, "max_range_male": max_m,
                            "min_range_female": min_f, "max_range_female": max_f,
                            "min_range_kids": min_k, "max_range_kids": max_k,
                            "print_order": len(current_test["parameters"]) + 1
                        })
    if current_test: all_tests.append(current_test)

    # FINAL EXPORT to the Standard Clinical Resources Location
    output_path = os.path.join(base_dir, 'src', 'main', 'resources', 'tests_seed.json')
    if not os.path.exists(os.path.dirname(output_path)):
        output_path = os.path.join(base_dir, 'tests_seed.json')
        
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(all_tests, f, indent=4)
    
    print(f"Sync Success: {len(all_tests)} clinical protocols synchronized.")

if __name__ == "__main__":
    sync_lab_data()
