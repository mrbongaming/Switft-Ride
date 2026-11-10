import { useState } from 'react';
import { Loader2, MapPin, Search } from 'lucide-react';
import { searchPlaces, type PlaceResult } from '../lib/map';

type Props = {
  label: string;
  placeholder: string;
  value: string;
  kind: 'pickup' | 'dropoff';
  disabled?: boolean;
  onChange: (value: string) => void;
  onSelect: (place: PlaceResult) => void;
  onError: (message: string) => void;
};

export function PlaceSearchField({ label, placeholder, value, kind, disabled, onChange, onSelect, onError }: Props) {
  const [results, setResults] = useState<PlaceResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [searched, setSearched] = useState(false);

  async function search() {
    if (value.trim().length < 3) {
      onError('Nhập ít nhất 3 ký tự để tìm địa chỉ.');
      return;
    }
    setSearching(true);
    setSearched(false);
    try {
      const nextResults = await searchPlaces(value);
      setResults(nextResults);
      setSearched(true);
    } catch (error) {
      setResults([]);
      onError(error instanceof Error ? error.message : 'Không tìm được địa chỉ.');
    } finally {
      setSearching(false);
    }
  }

  return <div className="relative">
    <label className="relative flex items-center gap-3 rounded-[14px] border border-slate-200 bg-white px-3 py-3.5 transition focus-within:border-brand focus-within:ring-2 focus-within:ring-blue-100">
      <span className="z-10 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-white text-brand">{kind === 'pickup' ? <span className="h-3 w-3 rounded-full border-[3px] border-brand" /> : <MapPin size={17} fill="#2463eb" strokeWidth={1.5} />}</span>
      <span className="min-w-0 flex-1">
        <span className="block text-[10px] font-bold uppercase tracking-wider text-muted">{label}</span>
        <input
          className="w-full bg-transparent pr-9 text-sm font-semibold outline-none placeholder:font-normal placeholder:text-slate-400"
          value={value}
          disabled={disabled}
          onChange={event => { onChange(event.target.value); setResults([]); setSearched(false); }}
          onKeyDown={event => { if (event.key === 'Enter') { event.preventDefault(); void search(); } }}
          placeholder={placeholder}
        />
      </span>
      <button type="button" disabled={disabled || searching} onClick={() => void search()} className="absolute right-3 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-lg text-brand transition hover:bg-blue-50" aria-label={`Tìm ${label.toLowerCase()}`}>
        {searching ? <Loader2 size={17} className="animate-spin" /> : <Search size={17} />}
      </button>
    </label>
    {(results.length > 0 || searched) && <div className="absolute left-0 right-0 top-[calc(100%+6px)] z-[1000] max-h-64 overflow-auto rounded-xl border border-slate-200 bg-white p-1.5 shadow-xl">
      {results.length ? results.map(place => <button key={place.id} type="button" onClick={() => { onSelect(place); setResults([]); setSearched(false); }} className="flex w-full items-start gap-2 rounded-lg px-3 py-2.5 text-left text-xs leading-relaxed transition hover:bg-blue-50"><MapPin size={15} className="mt-0.5 shrink-0 text-brand" /><span>{place.address}</span></button>) : <p className="px-3 py-4 text-center text-xs text-muted">Không tìm thấy địa chỉ phù hợp.</p>}
    </div>}
  </div>;
}
